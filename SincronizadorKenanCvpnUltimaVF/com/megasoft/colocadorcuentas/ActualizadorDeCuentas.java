/*Realizado por Alexis Rivas
 * 
 */
package com.megasoft.colocadorcuentas;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.megasoft.colocadorcuentas.GsmSinCuentaEnCvpn;

/**
 * <p>
 * Agrega el número de cuenta para los registros de la tabla VPN_CORP.NU_CUENTA
 * para aquellos gsms que no tiene cuenta asociada en cvpn.
 * </p>
 * <p>
 * Esto es porque ocurrió durante un tiempo, que para las vpns no se estaba
 * agregando el número de cuenta en la base de datos de cvpn. Esto fué
 * modificado hace tiempo y ahora si se toma la cuenta cuando se agrega una vpn
 * por la aplicación web de cvpn.
 * </p>
 * <p>
 * Lo que hace este programa es simplemente buscar las cuentas en Kenan para
 * cada GSM que no la tenga y actualizarla en la base de datos de cvpn. Las vpns
 * que queden sin cuenta son eliminadas de cvpn poniéndole el estado 'D'.
 * </p>
 * <p>
 * La aplicación utiliza un parámetro de línea de comandos que es el nombre de
 * un archivo de propiedades de java. Utiliza un segudno parámetro opcional, que
 * cuando es "commit" se guardarán los cambio en la BD y si no, solo se mostrará
 * lo que se hará.
 * </p>
 * <p>
 * Se utilizan dos conexiones de bases de datos, una conexión para Kenan y otra
 * para CVPN.
 * </p>
 * <p>
 * Al inicio del proceso se obtienen todas las cuentas de Kenan para traer la
 * lista de las cuentas con planes nexo; también se traen todas las vpns de
 * CVPN. Con estas dos listas se realizan todas las compraraciones para
 * determinar si se deben agregar o quitar elementos de una VPN.
 * </p>
 * <p>
 * La aplicación (y cuando se monta como proyecto en Eclipse) solo depende del
 * driver de la base de datos. En este momento es el driver de Oracle,
 * classes12.jar, que debe ser agregado al classpath.
 * </p>
 * 
 * @author Camilo Torres ctorres@megasoft.com.ve
 * 
 */
public class ActualizadorDeCuentas {
	/**
	 * Conexión hacia la base de datos de Kenan FX
	 */
	static Connection conexionKenan = null;

	/**
	 * Conexión hacia la base de datos de CVPN. Se abre al inicio del programa,
	 * es utilizada por todos los objetos y se cierra al final.
	 */
	static Connection conexionCvpn = null;

	/**
	 * Log de la aplicaicón. Se crea un archivo de log llamado
	 * actualizadorcuentas_n.txt, donde n es el número del log. Puede haber
	 * hasta 10 archivos de log de hasta 1MB c/u.
	 */
	static Logger log = null;

	/**
	 * Usuario que se coloca al momento de insertar o modificar un elemento de
	 * una VPN. Se utiliza porque es un dato importante al momento de realizar
	 * una auditoría de cambios en las tablas de cvpn. Debe ser un nombre corto
	 * de usuario que no exceda 20 caracteres. No es necesario que sea un
	 * usuario registrado o existente.
	 */
	static String usuarioQueCambiaLasVpns = null;

	/**
	 * Indica si se debe guardar los cambios en la BD o solo mostrar los
	 * posibles cambios. Se usa para probar el programa antes de correrlo
	 * completamente. Se setea según un parámetro de línea de comandos
	 */
	static boolean modificarEnBD = false;

	/**
	 * <p>
	 * Realiza las actividades principales de la apliación. Es el punto de
	 * entrada de la aplicación.
	 * </p>
	 * <p>
	 * Toma un único argumento, que es el nombre del archivo de propiedades de
	 * la aplicación.
	 * </p>
	 * <p>
	 * Las acciones básicas que realiza son:
	 * <li>Carga el archivo de propiedades.</li>
	 * <li>Abre las conexiones a las bases de datos: la de Kena y la de cvpn.</li>
	 * <li>Trae las cuentas de kenan y las actualiza en cvpn.</li>
	 * <li>Cierra las conexiones de la base de datos.</li>
	 * </p>
	 * 
	 * @param args
	 *            El primer argumento con el nombre del archivo de propiedades;
	 *            el segundo es opcional y si contien "commit" se guardarán los
	 *            cambios en la BD, si no no se guardan
	 */
	public static void main(String[] args) {
		// lee las propiedaes desde la entrada estándar
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(args[0]));

			usuarioQueCambiaLasVpns = prop.getProperty("vpnCorp.cambia");
		} catch (IOException e1) {
			System.err
					.println("No pudo leer las propiedades desde la entrada estandar");
			e1.printStackTrace();
		}

		// ver si se va a guardar en la BD o solo mostrar
		modificarEnBD = args.length > 1 ? args[1].equalsIgnoreCase("commit")
				: false;

		// crea el log
		try {
			abrirLog(prop);
		} catch (SecurityException e) {
			System.err
					.println("No se puede crear el log por permisos se deguridad");
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("No se puede escribir en el archivo de log");
			e.printStackTrace();
			System.exit(-1);
		}

		log.finest("Iniciando el proceso");

		// abre las conexiones de las bd
		try {
			abrirConexionesBD(prop);
		} catch (Exception e) {
			log.severe("Error al crear las conexiones");
			log.throwing("ActualizadorDeCuentas", "main", e);
			cerrarConexionesBD();
			System.exit(-1);
		}

		// actualiza las cuentas que estaban vacías
		try {
			colocarLasCuentasEnCvpn();
		} catch (SQLException e) {
			log.severe("Error al sincronizar");
			log.throwing("ActualizadorDeCuentas", "main", e);
			cerrarConexionesBD();
			System.exit(-1);
		}

		// cierra las conexiones
		cerrarConexionesBD();

		log.finest("Terminado el proceso");
	}

	/**
	 * Abre un archivo de log para llevar un registro de errores y de acciones
	 * de la aplicación. Si no existe el archivo, lo crea. Guarda máximo diez
	 * archivos de log de 1MB cada uno.
	 * 
	 * @param prop
	 *            Archivo de propiedades. De aquí saca el nivel de log
	 *            (nivelDeLog)
	 * @throws SecurityException
	 *             Si no puede crear el archivo de log por problemas de permisos
	 *             del filesystem.
	 * @throws IOException
	 *             Si no puede abrir o escribir en el archivo.
	 */
	private static void abrirLog(Properties prop) throws SecurityException,
			IOException {
		log = Logger
				.getLogger("com.megasoft.colocadorcuentas.ActualizadorDeCuentas");
		FileHandler fh = new FileHandler("actualizadorcuentas_%g.txt",
				512 * 2000, 10, true);
		fh.setLevel(Level.parse(prop.getProperty("nivelDeLog")));
		log.setLevel(Level.parse(prop.getProperty("nivelDeLog")));
		fh.setFormatter(new SimpleFormatter());
		log.addHandler(fh);
	}

	/**
	 * Abre las conexiones con las dos BD que se van a sincronizar.
	 * 
	 * @throws ClassNotFoundException
	 *             Si no consigue el driver de la BD
	 * @throws SQLException
	 *             Si no logra la conexión con la BD
	 */
	public static void abrirConexionesBD(Properties prop) throws Exception {
		log.finest("ENTRANDO");

		// carga el driver de la BD
		Class.forName(prop.getProperty("driverDeBD"));
		log.finest("Consiguio el driver de la BD");

		// crea la conexión con Kenan
		conexionKenan = DriverManager.getConnection(prop
				.getProperty("urlDeBDKenan"), prop
				.getProperty("usuarioDeBDKenan"), prop
				.getProperty("passwordDeBDKenan"));
		log.finest("Creo la conexión hacia Kenan");

		// crea la coneixón con CVPN
		conexionCvpn = DriverManager.getConnection(prop
				.getProperty("urlDeBDCvpn"), prop
				.getProperty("usuarioDeBDCvpn"), prop
				.getProperty("passwordDeBDCvpn"));
		conexionCvpn.setAutoCommit(false);
		log.finest("Creo la conexión hacia CVPN");

		log.finest("SALIENDO");
	}

	/**
	 * Cierra las conexión hacia Kenan FX
	 */
	public static void cerrarConexionesBD() {
		log.finest("Entrando");
		try {
			conexionKenan.close();
		} catch (SQLException e) {
			log.severe("No pudo cerrar la conexión con Kenan");
			log.throwing("SincornizadorKenaCvpn", "cerrarConexionesBD", e);
		}

		try {
			conexionCvpn.close();
		} catch (SQLException e) {
			log.severe("No pudo cerrar la conexión con CVPN");
			log.throwing("SincornizadorKenaCvpn", "cerrarConexionesBD", e);
		}
		log.finest("Saliendo");
	}

	/**
	 * Algunos gsms en las vpn de cvpn no tienen el número de cuenta, es decir
	 * que lo tienen null o en blanco. En ese caso hay que ponérselo, ya que si
	 * no se pone, el resto del proceso podría crear vpn duplicadas.
	 * 
	 * Los que queden sin cuenta serán borrados (colocar estado 'D').
	 * 
	 * @throws SQLException
	 *             si hay error en acceso a BD
	 * 
	 */
	private static void colocarLasCuentasEnCvpn() throws SQLException {
		log.finest("Entra");

		ArrayList listaDeGsmsKenan = new ArrayList();

		// obtener los gsm que tienen cuenta vacía
		String query = "select id_vpn, nu_msisdn from vpn_corp where in_status<>'D' and (nu_cuenta is null or nu_cuenta = '')";
		Statement stmtCvpn = conexionCvpn.createStatement();
		ResultSet rsCvpn = stmtCvpn.executeQuery(query);
		HashMap mapaGsmsSinCuenta = new HashMap();
		GsmSinCuentaEnCvpn gsm;
		while (rsCvpn.next()) {
			gsm = new GsmSinCuentaEnCvpn();
			gsm.setIdDeLaVpn(rsCvpn.getString("id_vpn"));
			gsm.setGsm(rsCvpn.getString("nu_msisdn"));
			mapaGsmsSinCuenta.put(gsm.getGsm(), gsm);
		}
		rsCvpn.close();
		stmtCvpn.close();

		// crear una lista de los GSMs que se traerá de kenan
		StringBuffer listaGsmsSinCuenta = new StringBuffer();
		Iterator iter = mapaGsmsSinCuenta.values().iterator();
		Statement stmtKenan = conexionKenan.createStatement();
		ResultSet rsKenan;
		int índiceParaCantidadDeCuentas = 1;
		while (iter.hasNext()) {
			if (listaGsmsSinCuenta.length() != 0) {
				listaGsmsSinCuenta.append(",");
			}
			listaGsmsSinCuenta.append("'").append(
					((GsmSinCuentaEnCvpn) iter.next()).getGsm()).append("'");

			/*
			 * En Oracle, la lista de cuentas que se puede poner en el query no
			 * puede exceder de 1000 cuentas, por eso hay que dividir la lista
			 * en varias partes y realizar varios queries cuando la lista sea
			 * muy grande
			 */
			if (índiceParaCantidadDeCuentas++ % 500 == 0) {
				// traer las cuentas de kenan para los gsms de la lista
				// se trae solo las que estén activas
				query = "select external_id, cmf.HIERARCHY_ID "
						+ "from external_id_equip_map_view map,cmf cmf "
						+ "where view_status=2 and active_date<sysdate "
						+ "and (inactive_date>sysdate or inactive_date is null) and external_id IN ("
						+ listaGsmsSinCuenta.toString()
						+ ") and map.account_no=cmf.account_no and cmf.DATE_ACTIVE<sysdate "
						+ "and (cmf.DATE_INACTIVE>sysdate or cmf.DATE_INACTIVE is null)";
				rsKenan = stmtKenan.executeQuery(query);
				while (rsKenan.next()) {
					gsm = new GsmSinCuentaEnCvpn();
					gsm.setGsm(rsKenan.getString("external_id"));
					gsm.setCuenta(rsKenan.getLong("HIERARCHY_ID"));
					listaDeGsmsKenan.add(gsm);
				}
				rsKenan.close();
				listaGsmsSinCuenta = new StringBuffer();
			}
		}
		/*
		 * traer las cuentas de kenan para los gsms de la lista se trae solo las
		 * que estén activas. Hay que hacerlo con las últimas cuentas que hayan
		 * quedado en la última lista
		 */
		if (listaGsmsSinCuenta.length() > 0) {
			query = "select external_id, cmf.HIERARCHY_ID "
				+ "from external_id_equip_map_view map,cmf cmf "
				+ "where view_status=2 and active_date<sysdate "
				+ "and (inactive_date>sysdate or inactive_date is null) and external_id IN ("
				+ listaGsmsSinCuenta.toString()
				+ ") and map.account_no=cmf.account_no and cmf.DATE_ACTIVE<sysdate "
				+ "and (cmf.DATE_INACTIVE>sysdate or cmf.DATE_INACTIVE is null)";
			rsKenan = stmtKenan.executeQuery(query);
			while (rsKenan.next()) {
				gsm = new GsmSinCuentaEnCvpn();
				gsm.setGsm(rsKenan.getString("external_id"));
				gsm.setCuenta(rsKenan.getLong("HIERARCHY_ID"));
				listaDeGsmsKenan.add(gsm);
			}
			rsKenan.close();
		}
		stmtKenan.close();

		/*
		 * ahora actualizar todos los gsms que se hayan traido de kenan. En el
		 * caso de que se haya encontrado el número de cuenta, se actualiza este
		 * número de cuenta en cvpn.
		 */
		PreparedStatement pstmtCvpn = conexionCvpn
				.prepareStatement("update vpn_corp set NU_CUENTA=?,DT_MOD=sysdate,TX_MOD_BY=? where ID_VPN=? and NU_MSISDN=?");
		iter = listaDeGsmsKenan.iterator();
		GsmSinCuentaEnCvpn gsmKenan, gsmCvpn;
		long índiceParaCommit = 1;
		try {
			if (iter.hasNext()) {
				StringBuffer modificaciones = new StringBuffer(
						"\nCuentas para agregar (vpn,gsm,cuenta):")
						.append("\n");
				while (iter.hasNext()) {
					// gsmKenan tiene el número de cuenta y el gsm
					gsmKenan = (GsmSinCuentaEnCvpn) iter.next();
					// gsmCvpn tiene le id de la vpn y el gsm
					gsmCvpn = (GsmSinCuentaEnCvpn) mapaGsmsSinCuenta
							.get(gsmKenan.getGsm());
					pstmtCvpn.setLong(1, gsmKenan.getCuenta());
					pstmtCvpn.setString(2, usuarioQueCambiaLasVpns);
					pstmtCvpn.setString(3, gsmCvpn.getIdDeLaVpn());
					pstmtCvpn.setString(4, gsmCvpn.getGsm());
					modificaciones.append("\t").append(gsmCvpn.getIdDeLaVpn())
							.append(",").append(gsmCvpn.getGsm()).append(",")
							.append(gsmKenan.getCuenta()).append("\n");
					if (modificarEnBD) {
						pstmtCvpn.executeUpdate();
						if (índiceParaCommit++ % 500 == 0) {
							conexionCvpn.commit();
						}
					}
				}
				log.severe(modificaciones.toString());
			}
			if (modificarEnBD) {
				conexionCvpn.commit();
			}
		} catch (SQLException e) {
			conexionCvpn.rollback();
			throw e;
		}
		pstmtCvpn.close();

		/*
		 * ahora va a borrar las que hayan quedado en null
		 */
		// obtener los gsm que tienen cuenta vacía
		query = "select id_vpn, nu_msisdn, id_user from vpn_corp where in_status<>'D' and (nu_cuenta is null or nu_cuenta = '')";
		stmtCvpn = conexionCvpn.createStatement();
		rsCvpn = stmtCvpn.executeQuery(query);
		// aquí se guardan los datos que se van a borrar
		ArrayList gsmsSinCuenta = new ArrayList();
		while (rsCvpn.next()) {
			gsm = new GsmSinCuentaEnCvpn();
			gsm.setIdDeLaVpn(rsCvpn.getString("id_vpn"));
			gsm.setGsm(rsCvpn.getString("nu_msisdn"));
			gsm.setIdUserSecuencial(rsCvpn.getString("id_user"));
			gsmsSinCuenta.add(gsm);
		}
		rsCvpn.close();
		stmtCvpn.close();

		StringBuffer modificaciones = new StringBuffer("\n");

		// traer los datos a borrar y borrarlos de staging y cvpn
		iter = gsmsSinCuenta.iterator();
		modificaciones
				.append("Registros a borrar (ID_VPN,ID_USER,NU_MSISDN):\n");
		while (iter.hasNext()) {
			gsm = (GsmSinCuentaEnCvpn) iter.next();
			modificaciones.append("\t").append(gsm.getIdDeLaVpn()).append(",")
					.append(gsm.getIdUserSecuencial()).append(",").append(
							gsm.getGsm()).append("\n");
			if (modificarEnBD) {
				try {
					// borrar los frecuentes de cvpn
					PreparedStatement pstmtBorrar = conexionCvpn
							.prepareStatement("DELETE FROM VIRTUAL_CORP WHERE ID_VPN=? AND ID_USER=?");
					pstmtBorrar.setString(1, gsm.getIdDeLaVpn());
					pstmtBorrar.setString(2, gsm.getIdUserSecuencial());
					pstmtBorrar.executeUpdate();
					pstmtBorrar.close();

					// borrar los frecuentes de staging
					pstmtBorrar = conexionCvpn
							.prepareStatement("DELETE FROM mds.lac_to_prefix@DBL_MDS4_HIBRIDO WHERE MSISDN_PREFIX = ? || ?");
					pstmtBorrar.setString(1, gsm.getIdDeLaVpn());
					pstmtBorrar.setString(2, gsm.getIdUserSecuencial());
					pstmtBorrar.executeUpdate();
					pstmtBorrar.close();

					// borrar los miembros en staging
					pstmtBorrar = conexionCvpn
							.prepareStatement("DELETE FROM mds.ACCOUNT_TCOM_LOOKUP_TABLE@DBL_MDS4_HIBRIDO WHERE PLAN = ? || ?");
					pstmtBorrar.setString(1, gsm.getIdDeLaVpn());
					pstmtBorrar.setString(2, gsm.getIdUserSecuencial());
					pstmtBorrar.executeUpdate();
					pstmtBorrar.close();

					// borra de las tablas de CVPN
					PreparedStatement stmtActualizarMiembros = conexionCvpn
							.prepareStatement("UPDATE VPN_CORP SET in_status='D' where id_vpn=? AND NU_MSISDN=?");
					stmtActualizarMiembros.setString(1, gsm.getIdDeLaVpn());
					stmtActualizarMiembros.setString(2, gsm.getGsm());
					stmtActualizarMiembros.executeUpdate();
					stmtActualizarMiembros.close();

					// commit de todos los cambios
					conexionCvpn.commit();
				} catch (Exception exception) {
					conexionCvpn.rollback();
				}
			}

			log.severe(modificaciones.toString());

			/*
			 * si queda alguno sin número de cuenta, se debe borrar o colocar
			 * con estado D en CVPN.
			 */
			if (modificarEnBD) {
				stmtCvpn = conexionCvpn.createStatement();
				String update = "update vpn_corp set IN_STATUS='D' where NU_CUENTA is null";
				try {
					stmtCvpn.executeUpdate(update);
					conexionCvpn.commit();
				} catch (SQLException e) {
					conexionCvpn.rollback();
					throw e;
				}
				stmtCvpn.close();
			}
		}
		log.finest("Sale");
	}
}

