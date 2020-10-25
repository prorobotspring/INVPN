# ejecuta el sincronizador en modo commit
# modifica las vpns en la base de datos y luego sincroniza
# en el staging usando el stored procedure de sincronizacion
# esta version ejecuta tambien las acciones de borrado de vpns y gsms

java -cp classes:classes12.jar com.megasoft.sincronizador.SincronizadorKenanCvpn sincronizador.properties commit borrar > log/resultado_sinc.txt

