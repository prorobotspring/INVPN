#!/usr/bin/ksh -ux

# ejecuta el sincronizador en modo commit y aprovisiona en INVPN
# modifica las vpns en la base de datos y luego sincroniza
# en el staging usando el stored procedure de sincronizacion
# esta version ejecuta tambien las acciones de borrado de vpns y gsms



THIS_SCRIPT=$(basename $0)
NAME_THIS_SCRIPT=${THIS_SCRIPT%.*}
SYSDATE=$(date +'%Y%m%d%H%M%S')
HOY=$(date +'%Y%m%d')
MES=$(date +'%Y%m')


# asignar cuenta de notificacion de fallas
MAIL=postpago@digitel.com.ve
# comentar esta linea para que les llegue alarmas de ejecucion
MAIL=

: ${MODULE_NAME:=SincronizadorKenanCvpn}
: ${ROOT_PATH:=/opt/kenan/scripts}
#ROOT_PATH=$PWD
APPL_PATH=${ROOT_PATH}/${MODULE_NAME}

TMP_PATH=${APPL_PATH}/tmp
LOG_PATH=${APPL_PATH}/log

# se asegura la existencia de las carpetas
[ -d ${TMP_PATH} ] || mkdir -p ${TMP_PATH}
[ -d ${LOG_PATH} ] || mkdir -p ${LOG_PATH}


LOCK_FILE=${APPL_PATH}/${MODULE_NAME}.pid
LOG_FILE=${LOG_PATH}/${NAME_THIS_SCRIPT}_${MES}.log

STDOUT=${LOG_PATH}/resultado_sinc.txt

CLASSPATH=classes:$(ls lib/*.jar|sed 's/$/:/'|paste -s -|tr -d "[[:space:]]")

JAVA="java"
[ -n "${JAVA_HOME}" ] && JAVA=${JAVA_HOME}/${JAVA}

JAVA_CMD="${JAVA} -cp $CLASSPATH"

# Verifica que no corra dos veces
if [ -f "${LOCK_FILE}" ] ; then
    if ps -p $(head -1 ${LOCK_FILE}) ; then
        :
    else
        rm -f ${LOCK_FILE}
    fi
fi

if [ -f "${LOCK_FILE}" ] ; then
    if [ -n "${MAIL}"  ] ; then 
        print "Advertencia. Existe un proceso de sincronizacion automatica de VPN en curso cuando no se esperaba.\n    Proceso: ${THIS_SCRIPT}"|
        mailx -s "ADVERTENCIA: Procesos en ejecuci?n:  ${NAME_THIS_SCRIPT}" ${MAIL}
    fi
    exit 2
fi

exec >>${STDOUT} 2>&1

echo "########################## ${SYSDATE} ########################33"
set

echo "Crea el lockfile y se asegura que se borre al salir"
echo $$         >${LOCK_FILE}
echo $0         >>${LOCK_FILE}
echo ${SYSDATE} >>${LOCK_FILE}

trap "rm -f ${LOCK_FILE}" EXIT 


ejecuta(){
set -x
    CMD="$@"
    echo "${SYSDATE}:I:$CMD">>${LOG_FILE}
    eval $CMD 2>>${LOG_FILE}
    RESULT=$?
    if [ ${RESULT} -eq 0 ] ; then 
        print "${SYSDATE}:I:Ok: $CMD" >>${LOG_FILE}
    else
        print "${SYSDATE}:E:Error ejecutando: $CMD" >>${LOG_FILE}
        if [ -n "${MAIL}"  ] ; then 
            print "Proceso: ${THIS_SCRIPT}\n${SYSDATE}:E:Error ejecutando: $CMD.    "|
            mailx -s "ERROR: Falla en conciliacion Kenan/CVPN:  ${NAME_THIS_SCRIPT}" ${MAIL}
        fi
    fi
    return $RESULT
}

rm -f ${STDOUT}


print "${SYSDATE}:I:Inicio">>${LOG_FILE}

# Fase1: Marca Status I/U/D
# Fase2: Provisioning INVPN y HLR
# Fase3: Provisioning Mediaci?n

cd ${APPL_PATH}

ACCION=commit
#ACCION=prueba
CFG=resource/sincronizador.properties

ejecuta ${JAVA_CMD} com.megasoft.sincronizador.SincronizadorKenanCvpn ${CFG} ${ACCION} noborrar fase1  >> ${STDOUT} &&
ejecuta ${JAVA} -jar provisioning.jar resource/ccinvpn.properties bd/selectCuentasVpn.sql resource/log4j.xml >> ${STDOUT} && 
ejecuta ${JAVA_CMD} com.megasoft.sincronizador.SincronizadorKenanCvpn ${CFG} ${ACCION} borrar fase3 >> ${STDOUT} 

print "${SYSDATE}:X:Fin">>${LOG_FILE}

