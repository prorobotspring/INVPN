package com.megasoft.sincronizador;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * <p>
 * Sirve para sincronizar o para mantener consistente la información entre la
 * base de datos de cvpn y la base de datos de kenan fx.
 * </p>
 * <p>
 * El propósito de la aplicación es realizar automáticamente la sincronización
 * desde Kenan hacia CVPN, es decir que la base de datos de cvpn debe
 * actualizarce según lo que está en Kenan. Los objetivos son:
 * <li>Identificar activaciones de líneas y crearlas en la base de datos de la
 * aplicación si aun no han sido incluidas. Las líneas deben ser incluidas en la
 * VPN correcta.</li>
 * <li>Identificar cambios de numero y actualizarlos en la base de datos de la
 * aplicación si aun no han sido actualizados en su VPN respectiva. Esto se
 * refiere a los números que son cambiados de cuenta, es decir, antes estaban en
 * una cuenta y ahora en otra.</li>
 * <li>Identificar anulaciones y/o inactivaciones de líneas para ser eliminadas
 * de la base de datos de la aplicación y sacadas de la vpn donde estaba.</li>
 * </p>
 * <p>
 * En la base de datos propia de la aplicación de cvpn se guardan las vpns en la
 * tabla vpn_corp. También existe un área de stage para controlar o manejar el
 * aprovicionamiento de la vpn en mediación; esta área de stage es accesible a
 * través de un dblink en la misma base de datos de cvpn. Es importante saber
 * que la aplicación NO modifica directamente las tablas de stage para modificar
 * las vpn en mediación. Para actualizar en el staging se utiliza el stored
 * procedure ACTUALIZAR_VPN_CORPORATIVA.
 * </p>
 * <p>
 * Para lograr los objetivos de la aplicación, lo que se hace es ver el proceso
 * desde la siguiente perspectiva: Hay que hacer que las vpn que se encuentran
 * en la base de datos de cvpn, estén configuradas según están en un momento
 * dado las cuentas y los gsms de esas cuentas en Kenan FX. Dicho de otra forma,
 * hay que buscar las cuentas y gsms en Kenan, y ver que en cvpn estén igual,
 * quitando lo que sobre y agregando lo que falte.
 * </p>
 * <p>
 * La aplicación está compuesta por tres clases: El sincronizador y las cuentas,
 * una para kenan y otra para cvpn.
 * </p>
 * <p>
 * El sincronizador es esta clase y es quien maneja la sesión y gran parte de la
 * lógica del proceso está en esta clase.
 * </p>
 * <p>
 * La cuenta kenan representa a una cuenta activa en Kenan, que además tiene un
 * plan de tipo Nexo que son los que se utilizan en cvpn. También contiene una
 * lista con todos sus GSMs activos en ese momento.
 * </p>
 * <p>
 * La cuenta cvpn representa una vpn en cvpn. Contiene los GSMs de la vpn y
 * otros datos básicos de la misma.
 * </p>
 * <p>
 * El algoritmo simplificado para la sincronización consiste en tres pasos, uno
 * de los cuales se divide en dos pasos distintos:
 * <li>Borrar todas las vpns que no tengan cuentas activas con planes nexo en
 * kenan.</li>
 * <li>Para las vpns que tengan sus cuentas en kenan con plan nexo, sincronizar
 * sus GSMs, que consiste en:</li>
 * <ul>
 * <li>Borrar todos los GSMs que estén en la VPN y que ya no estén en la cuenta
 * en Kenan.</li>
 * <li>Agregar los GSMs a la VPN cuando estén en la cuenta en Kenan y no estén
 * en la VPN.</li>
 * </ul>
 * <li>Agregar las VPNs para aquellas cuentas que no tengan VPN en cvpn pero
 * que si están en Kenan con plan Nexo</li>
 * <li>Llamar al stored procedure ACTUALIZAR_VPN_CORPORATIVA, el cual se
 * encarga de sincronizar con las tablas de staging.</li>
 * <li>No se ejecuta ningún borrado en la BD de cvpn, lo que se hace es colocar
 * los estados correctos en las filas, para que el stored procedure modifique en
 * staging.</li>
 * </p>
 * <p>
 * La aplicación utiliza un parámetro de línea de comandos que es el nombre de
 * un archivo de propiedades de java. Utiliza el segundo parámetro, el cual es
 * opcional y puede obtener el valor "commit" para indicar que se guarden los
 * cambios calculados en el proceso en la base de datos. Utiliza el segundo
 * parámetro de línea de comandos para controlar si debe realizar acciones de
 * borrado en la BD de cvpn. Si el segundo parámetro es "borrar" realizará las
 * acciones de borrado; si no, no realizará ninguna acción de borrado.
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
 * @author Alexis Rivas arivas@megasoft.com.ve
 * 
 */
public class SincronizadorKenanCvpn {
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
	 * Log de la aplicación. La aplicación genera un archivo de log, denominado
	 * log_n.log, donde n es un número de log.
	 */
	static Logger log = null;

	/**
	 * Lista que contiene todas las cuentas que están activas en Kenan y que
	 * tienen un plan de tipo nexo o un plan válido en CVPN. Los planes válidos
	 * en cvpn están en la tabla VPN_PLANES_KENAN. Cada cuenta contiene una
	 * lista con todos sus GSMs activos en Kenan.
	 */
	static ArrayList listaDeCuentasKenan = null;

	/**
	 * Lista que contiene todas las vpns, agrupadas por número de cuenta, que
	 * están actualmente configuradas en CVPN. Estas son las que deben ser
	 * ajustadas para coincidir con lo que está en Kenan.
	 */
	static ArrayList listaDeCuentasCvpn = null;

	/**
	 * Nota que se coloca al momento de insertar una VPN o un miembro de una
	 * vpn. Se obtiene del archivo de propiedades. Es un comentario que indique
	 * la acción de creación de la vpn. Se coloca porque es un dato que está en
	 * la tabla VPN_CORP.
	 */
	static String notaParaIserciónDeVpn = null;

	/**
	 * Usuario que se coloca al momento de insertar o modificar un elemento de
	 * una VPN. Se utiliza porque es un dato importante al momento de realizar
	 * una auditoría de cambios en las tablas de cvpn. Debe ser un nombre corto
	 * de usuario que no exceda 20 caracteres. No es necesario que sea un
	 * usuario registrado o existente.
	 */
	static String usuarioQueCambiaLasVpns = null;

	/**
	 * Número del plan con el cual se insertará una nueva vpn en la base de
	 * datos de cvpn. Se utiliza porque en cvpn se puede seleccionar un plan
	 * para la vpn, pero no hay forma de obtenerlo de forma automática, ya que
	 * no se conocen los códigos de plan en cvpn que correspondan con los planes
	 * o paquetes de Kenan. Es un número de VPN_CORP_CONFIG.ID_PLAN.
	 */
	static String planParaIserciónDeVpn = null;

	/**
	 * Indica si se deben realizar las modificaciones en la base de datos o no.
	 * Se utiliza para tener una opción en el programa que permita realizar una
	 * corrida sin modificar nada, para poder evaluar primero si la corrida
	 * funciona bien o no.
	 */
	static boolean modificarEnBD = false;

	/**
	 * Indica si se deben realizar acciones de borrado en las vpns. En el caso
	 * en que no se quiera que la aplicación borre nada de las vpns, sino que
	 * solo agregue las nuevas vpns y los nuevos gsms, se debe pasar un segundo
	 * parámetro que indique noborrar. Si se quiere que si borre las vpns y
	 * gsms, pasar un segundo parámetro borrar.
	 */
	static boolean borrarVpnsYGsms = false;

	/**
	 * Representa las propiedades de la aplicación. Se leen del archivo de
	 * propiedades que se pasa por parámetro
	 */
	static Properties prop = null;

	/**
	 * Contiene los planes de cvpn. La clave es el nombre del plan, el valor es
	 * el número del plan. Se utiliza para agregar el número del plan a las vpns
	 * nuevas. La clave es el nombre del plan, porque el nombre del plan es lo
	 * que viene de kenan y es el único valor para comparar.
	 */
	static HashMap mapaDePlanes = new HashMap();

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
	 * <li>Sincroniza la base de datos de cvpn con la de kenan.</li>
	 * <li>Cierra las conexiones de la base de datos.</li>
	 * </p>
	 * 
	 * @param args
	 *            El primer argumento con el nombre del archivo de propiedades,
	 *            el segundo es opcional y puede tener el valor "commit" para
	 *            que se realicen los cambios efectivamente en la base de datos.
	 */
	public static void main(String[] args) {
		// lee las propiedaes desde la entrada estándar
		System.out.println("dentro del main!!!!!");
		prop = new Properties();
		try {
			
			System.out.println("dentro del try!!!!");
			prop.load(new FileInputStream(args[0]));

			notaParaIserciónDeVpn = prop.getProperty("vpnCorp.nota");
			System.out.println("notaParaIserciónDeVpn: "+ notaParaIserciónDeVpn);
			usuarioQueCambiaLasVpns = prop.getProperty("vpnCorp.cambia");
			System.out.println("usuarioQueCambiaLasVpns: "+ usuarioQueCambiaLasVpns);
			planParaIserciónDeVpn = prop.getProperty("vpnCorp.plan");
			System.out.println("planParaIserciónDeVpn: "+ planParaIserciónDeVpn);
		} catch (IOException e1) {
			System.err
					.println("No pudo leer las propiedades desde la entrada estandar");
			e1.printStackTrace();
		}

		/*
		 * Ver si se va a modificar o no en la BD.
		 */
		if (args.length > 1) {
			modificarEnBD = args[1].equalsIgnoreCase("commit");
			System.out.println("entro en el args >1");
		}
		/*
		 * Ver si se van a ejecutar las acciones de borrado del proceso
		 */
		if (args.length > 2) {
			borrarVpnsYGsms = args[2].equalsIgnoreCase("borrar");
			System.out.println("entro en el args >2");
		}

		// crear o abrir el archivo de log
		try {
			System.out.println("dentro del try para crear el archivo log");
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
		log.info("inicio del proceso!!!!!!!!!!!!!");
		System.out.println("inicio del proceso!!!!!!!!!!!!!");
		

		/*
		 * Coloca en el log los parámetros de ejecución de la aplicación
		 */
		if (modificarEnBD) {
			log.info("Se va a modificar en la BD");
			System.out.println("Se va a modificar en la BD");
		} else {
			log.info("No se modifica la BD. Se corre en modo test");
			System.out.println("No se modifica la BD. Se corre en modo test");
		}
		if (borrarVpnsYGsms) {
			log.info("Se eligio borrar las vpns y gsms "
					+ "sobrantes en la bd de cvpn");
		} else {
			log.info("No se ejecutaran las acciones de "
					+ "borrado de vpns y gsms sobrantes");
		}

		// abrir las conexiones a la base de datos
		try {
			abrirConexionesBD(prop);
		} catch (Exception e) {
			log.severe("Error al crear las conexiones");
			log.throwing("SincronizadorKenanCvpn", "main", e);
			cerrarConexionesBD();
			System.exit(-1);
		}

		try {
			cargarMapaDePlanes();
		} catch (SQLException e1) {
			log.severe("Error al traer el mapa desde la BD");
			log.throwing("SincronizadorKenanCvpn", "main", e1);
			cerrarConexionesBD();
			System.exit(-1);
		}

		// sincronizar cvpn con kenan
		try {
			sincronizarBDKenanConCvpn();
		} catch (SQLException e) {
			log.severe("Error al sincronizar"+ e.getMessage());
			log.throwing("SincronizadorKenanCvpn", "main", e);
			cerrarConexionesBD();
			System.exit(-1);
		}

		// cerrar las conexiones de base de datos
		cerrarConexionesBD();

		log.finest("Fin del proceso");
	}

	/**
	 * Carga el mapa de planes de la aplicación. La clave del mapa es el nombre,
	 * porque el nombre del plan es lo que tienen las cuentas de kenan
	 * 
	 * @throws SQLException
	 *             error en la bd
	 */
	private static void cargarMapaDePlanes() throws SQLException {
		Statement stmt = conexionCvpn.createStatement();
		ResultSet rs = stmt.executeQuery(prop
				.getProperty("cvpn.query.CargarMapaDePlanes"));
		while (rs.next()) {
			mapaDePlanes.put(rs.getString("TX_PLAN"), rs.getString("ID_PLAN"));
		}
		rs.close();
		stmt.close();
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
				.getLogger("com.megasoft.sincronizador.SincromizadorKenanCvpn");
		FileHandler fh = new FileHandler("log_%g.txt", 512 * 2000, 10, true);
		fh.setLevel(Level.parse(prop.getProperty("nivelDeLog")));
		log.setLevel(Level.parse(prop.getProperty("nivelDeLog")));
		fh.setFormatter(new SimpleFormatter());
		log.addHandler(fh);
	}

	/**
	 * Abre las conexiones con las dos BD que se van a sincronizar.
	 * 
	 * @throws Exception
	 *             Si no consigue el driver de la BD, Si no logra la conexión
	 *             con la BD
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
	 * Cierra las conexión hacia Kenan FX.
	 */
	public static void cerrarConexionesBD() {
		log.finest("Entra");
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
		log.finest("Sale");
	}

	/**
	 * <p>
	 * Sincroniza las VPNs, para que todas las cuentas que están en Kenan con un
	 * plan de CVPN estén también en el sistema de CVPN y configurados en los
	 * sistemas de mediación.
	 * </p>
	 * <p>
	 * Primero borra las VPNs que están en CVPN, pero que ya en Kenan sus
	 * cuentas no tiene un plan del tipo correcto para cvpn.
	 * </p>
	 * <p>
	 * Segundo borra los GSMs de las VPNs que ya no estén asociados a su
	 * respectiva cuenta en Kenan
	 * </p>
	 * <p>
	 * Tercero, agrega los GSMs a la VPN correspondiente a una cuenta que en
	 * Kenan se le ha agregado ese GSM
	 * </p>
	 * <p>
	 * Cuarto y último, agrega las cuentas a las que se le ha asignado un plan
	 * de cvpn en Kenan y que aún no aparecen en CVPN.
	 * </p>
	 * 
	 * @throws SQLException
	 *             Si ocurre un error accediendo a la BD.
	 */
	public static void sincronizarBDKenanConCvpn() throws SQLException {
		log.finest("Entra");

		/*
		 * validar estado de la BD. Si hay ciertas inconsistencias no debe
		 * continuar el procesamiento
		 */
		if (!validaLaBD()) {
			log
					.severe("ERROR: temina porque hay inconsistencias en la BD de CVPN.");
			return;
		}
		log.info("antes de listaDeCuentasKenan");
		// traer las cuentas de Kenan
		listaDeCuentasKenan = getListaDeCuentasKenan();
		log.info("paso por la listaDeCuentasKenan");

		// traer las cuentas de CVPN
		listaDeCuentasCvpn = getListaDeCuentasCvpn();
		log.info("paso por la listaDeCuentasCvpn");

		System.out.println("***********************************************");
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("++ Comienzo del proceso ++");
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");

		// borrar las cuentas de CVPN que sobren
		log.info("antes de borrar las sobrantes");
		if (borrarVpnsYGsms) {
			log
					.finest("Se selecciono borrar las vpns sobrantes de la bd de cvpn.");
			borrarLasCuentasQueSobranEnCvpn();
		}

		// sincronizar los GSM de las cuentas existentes
		log.info("antes sincronizar los GSM de las cuentas existentes");
		sincronizarLosGsmsDeLasVpnsExistentes();

		// agregar las cuentas nuevas en Kenan hacia CVPN
		log.info("antes agregar las cuentas nuevas en Kenan hacia CVPN");
		agregarLasCuentasNuevasEnCvpn();
		
		log.info("despues agregar las cuentas nuevas en Kenan hacia CVPN");

		/*
		 * El stored procedure 'Actualizar_Vpn_Corporativa' se encuentra en la
		 * base de datos de cvpn (ya existía previamente) y se utiliza para
		 * actualizar las tablas de mediación con los cambios que ya están
		 * escritos en las tablas de cvpn. Se hace una llamada a este sp porque
		 * él es quien debe tocar las tablas de mediación
		 */
		if (modificarEnBD) {
			CallableStatement call = conexionCvpn
					.prepareCall("{ call Actualizar_Vpn_Corporativa }");
			call.execute();
			log.finest("Llamo al SP Actualizar_Vpn_Corporativa");
			call.close();
		}

		System.out.println("***********************************************");
		System.out.println("-----------------------------------------------");
		System.out.println("-- Fin del proceso --");
		System.out.println("-----------------------------------------------");
		System.out
				.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

		log.finest("Sale");
	}

	/**
	 * <p>
	 * Valida si la BD de CVPN está en buenas condiciones para procesar.
	 * </p>
	 * <p>
	 * Lo que se verifica es:
	 * <li>Si todavía hay registros en VPN_CORP con NU_CUENTA en null.</li>
	 * <li>Si hay VPNs con más de una cuenta en VPN_CORP.</li>
	 * <li>Si hay Cuentas que están en más de una vpn en VPN_CORP.</li>
	 * </p>
	 * 
	 * @return true si es válido para procesar, false si no
	 * @throws SQLException
	 *             Si hay error en acceso a BD.
	 */
	private static boolean validaLaBD() throws SQLException {
		boolean pasaLaValidación = true;

		// valida primero si hay vpns con cuentas en null
		Statement stmtValidación = conexionCvpn.createStatement();
		ResultSet rsValidación = stmtValidación
				.executeQuery("select count(1) from vpn_corp where in_status <> 'D' AND (nu_cuenta is null or trim(nu_cuenta) = '')");
		if (rsValidación.next() && rsValidación.getInt(1) > 0) {
			pasaLaValidación = false;
			log
					.severe("Todavía hay registros en VPN_CORP con NU_CUENTA en null. Revisar.");
		}
		rsValidación.close();

		// validar vpns con más de una cuenta
		rsValidación = stmtValidación
				.executeQuery("SELECT id_vpn,count(distinct nu_cuenta) "
						+ "FROM VPN_CORP WHERE nu_cuenta is not null "
						+ "AND  in_status <> 'D' " + "group by id_vpn "
						+ "HAVING count(distinct nu_cuenta) > 1");
		if (rsValidación.next()) {
			pasaLaValidación = false;
			log
					.severe("Hay VPNs con más de una cuenta en VPN_CORP. Revisar y corregir antes de poder continuar.");
			StringBuffer errores = new StringBuffer("\n");
			do {
				errores.append("\tVPN: ").append(rsValidación.getString(1))
						.append(" ").append(rsValidación.getInt(2)).append(
								" veces\n");
			} while (rsValidación.next());
			log.severe(errores.toString());
		}

		// validar cuentas que estén en más de una vpn
		rsValidación = stmtValidación
				.executeQuery("SELECT nu_cuenta,count(distinct id_vpn) "
						+ "FROM VPN_CORP WHERE nu_cuenta is not null "
						+ "AND  in_status <> 'D' " + "group by nu_cuenta "
						+ "HAVING count(distinct id_vpn) > 1");
		if (rsValidación.next()) {
			pasaLaValidación = false;
			log
					.severe("Hay Cuentas que están en más de una vpn en VPN_CORP. Revisar y corregir antes de poder continuar.");
			StringBuffer errores = new StringBuffer("\n");
			do {
				errores.append("\tCuenta: ").append(rsValidación.getString(1))
						.append(" ").append(rsValidación.getInt(2)).append(
								" vpns\n");
			} while (rsValidación.next());
			log.severe(errores.toString());
		}

		return pasaLaValidación;
	}

	/**
	 * Agrega las cuentas, insertando una vpn con sus gsms, cuando la cuenta se
	 * encuentra en kenan y no está en cvpn. Es el paso final del proceso.
	 * 
	 * @throws SQLException
	 *             Si no puede insertar las vpns en la base de datos
	 * 
	 */
	private static void agregarLasCuentasNuevasEnCvpn() throws SQLException {
		log.finest("Entra");

		// TODO Cambio para ampliar el campo id_user
		/*
		 * En algún momento pedirán la ampliación del campo id_user, es decir, que en
		 * vez de tener longitud 3, tenga longitud 5. Uno de los cambios es este
		 * query, que debe pasar de tener LPAD(?,3,'0') a LPAD(?,5,'0')
		 */
		// para reutilizar el statement de inversión
		/*PreparedStatement pstmtInsertarCvpn = conexionCvpn
				.prepareStatement("INSERT INTO CVPN.VPN_CORP ("
						+ "ID_VPN, TX_COMPANY, NU_MSISDN, "
						+ "ID_USER, IN_STATUS, ID_PLAN, "
						+ "TX_MOD_BY, DT_MOD, TX_NOTE, "
						+ "DT_CREACION, NU_CUENTA) "
						+ "VALUES (?,?,?,LPAD(?,3,'0'),'I',?,?,?,?,?,?)");*/
		
		
		
		
		// para reutilizar el statement de inversión
		String AmpliarId_User=SincronizadorKenanCvpn.prop.getProperty("ampliacion.IdUser");
		System.out.println("cadena para ampliar el id_user en el sincronizadorKenanCvpn: "+ AmpliarId_User);
	
		PreparedStatement pstmtInsertarCvpn = conexionCvpn
		.prepareStatement("INSERT INTO CVPN.VPN_CORP ("
				+ "ID_VPN, TX_COMPANY, NU_MSISDN, "
				+ "ID_USER, IN_STATUS, ID_PLAN, "
				+ "TX_MOD_BY, DT_MOD, TX_NOTE, "
				+ "DT_CREACION, NU_CUENTA) "
				+ "VALUES (?,?,?,"+AmpliarId_User+",'I',?,?,?,?,?,?)");
		
		
		
		
	

		// fecha de hoy para insertar en todos los campos de fecha
		java.sql.Date fechaDeModificación = new java.sql.Date(new Date()
				.getTime());

		/*
		 * Recorre la lista de cuentas en kenan, por cada una de ellas, busca a
		 * ver si NO está en la lista de cuentas de cvpn. Si no está, enconces
		 * debe agregarla.
		 */
		// aquí busca todas las cuentas de kenan
		Iterator iterKenan = listaDeCuentasKenan.iterator();
		CuentaKenan cuentaKenan;
		while (iterKenan.hasNext()) {
			boolean insertarVpn = true;
			cuentaKenan = (CuentaKenan) iterKenan.next();
			// aquí busca las cuentas de cvpn
			Iterator iterCvpn = listaDeCuentasCvpn.iterator();
			CuentaCvpn cuentaCvpn;
			while (iterCvpn.hasNext()) {
				cuentaCvpn = (CuentaCvpn) iterCvpn.next();
				// si la encuentra, no debe agregarla, pasa al siguiente
				if (cuentaKenan.getNumeroDeCuenta() == cuentaCvpn
						.getNumeroDeCuentaCvpn()) {
					insertarVpn = false;
					break;
				}
			}

			/*
			 * Agrega la cuenta nueva solo si contiene más de un GSM
			 */
			if (cuentaKenan.getListaDeGsmsActivos().size() < 2) {
				insertarVpn = false;
			}

			/*
			 * Si econtró una cuenta que no estaba en cvpn, entonces debe ser
			 * agregada
			 */
			if (insertarVpn) {
				int secuencialParaIdUser = 1;
				// obtener el id de la VPN de la secuencia de la BD
				// se usa para que cada vpn tenga un id único
				String query = "select lpad(VPN_CORP_SEQ.nextval,5,'0') from dual";
				Statement stmt = conexionCvpn.createStatement();
				ResultSet rs = stmt.executeQuery(query);

				System.out.println("Agrega nueva VPN para cuenta: "
						+ cuentaKenan.getNumeroDeCuenta());

				if (rs.next()) {
					// aquí está el id de la vpn
					String idDeLaVpn = rs.getString(1);
					rs.close();
					stmt.close();

					// inserta la vpn con todos sus gsms
					Iterator listaDeGsms = cuentaKenan.getListaDeGsmsActivos()
							.iterator();
					// para hacer commit cada 500 registros insertados
					int índiceParaCommit = 1;

					// modifica en bd solo si se le indica
					while (listaDeGsms.hasNext()) {
						String gsm = (String) listaDeGsms.next();

						/*
						 * traer el nombre de la empresa. El nombre de la
						 * empresa no se tiene hasta este momento. Se toma de la
						 * tabla CMF
						 */
						PreparedStatement stmtNombreEmpresa = conexionKenan
								.prepareStatement(prop
										.getProperty("cvpn.query.TraerNombreDeLaEmpresaParaCuenta"));
						stmtNombreEmpresa.setLong(1, cuentaKenan
								.getNumeroDeCuenta());
						ResultSet rsNombreEmpresa = stmtNombreEmpresa
								.executeQuery();
						if (rsNombreEmpresa.next()) {
							cuentaKenan.setNombreDeLaEmpresa(rsNombreEmpresa
									.getString(1));
						} else {
							cuentaKenan
									.setNombreDeLaEmpresa("No se consigue el nombre en Kenan");
						}
						rsNombreEmpresa.close();
						stmtNombreEmpresa.close();

						// para el log
						System.out.println("\t" + idDeLaVpn + ","
								+ cuentaKenan.getNombreDeLaEmpresa() + ","
								+ gsm + "," + (secuencialParaIdUser + 1) + ","
								+ planParaIserciónDeVpn + ","
								+ usuarioQueCambiaLasVpns + ","
								+ fechaDeModificación.toGMTString() + ","
								+ notaParaIserciónDeVpn + ","
								+ fechaDeModificación + ","
								+ cuentaKenan.getNumeroDeCuenta());

						if (modificarEnBD) {
							try {
								// para insertar en cvpn
								pstmtInsertarCvpn.setString(1, idDeLaVpn);
								pstmtInsertarCvpn.setString(2, cuentaKenan
										.getNombreDeLaEmpresa());
								pstmtInsertarCvpn.setString(3, gsm);
								pstmtInsertarCvpn.setString(4, Integer
										.toString(secuencialParaIdUser++));
								/*
								 * Coloca el número de plan según se lo trajo de
								 * kenan, si no tiene plan, coloca uno por
								 * defecto que está configurado en el archivo de
								 * propiedades
								 */
								pstmtInsertarCvpn
										.setString(
												5,
												cuentaKenan.getNúmeroDelPlan() != null ? cuentaKenan
														.getNúmeroDelPlan()
														: planParaIserciónDeVpn);
								pstmtInsertarCvpn.setString(6,
										usuarioQueCambiaLasVpns);
								pstmtInsertarCvpn.setDate(7,
										fechaDeModificación);
								pstmtInsertarCvpn.setString(8,
										notaParaIserciónDeVpn);
								pstmtInsertarCvpn.setDate(9,
										fechaDeModificación);
								pstmtInsertarCvpn.setInt(10, cuentaKenan
										.getNumeroDeCuenta());
								pstmtInsertarCvpn.executeUpdate();

								// hace commit cada 500 registros insertados
								if (índiceParaCommit++ % 500 == 0) {
									conexionCvpn.commit();
								}
							} catch (SQLException e) {
								conexionCvpn.rollback();
								throw e;
							}
						}
					}
					// hace commit con los últimos registros
					// insertados
					conexionCvpn.commit();
				} else {
					rs.close();
					stmt.close();
				}
			}
		}
		pstmtInsertarCvpn.close();

		log.finest("Sale");
	}

	/**
	 * Sincroniza los GSMs de las cuentas que están en kenan y en cvpn al mismo
	 * tiempo. Este es el tercer paso del proceso. Para cada cuenta en Kenan y
	 * que también esté en cvpn, debe sincronizar sus GSMs.
	 * 
	 * @throws SQLException
	 *             Si da un error insertando o actualizando en la BD
	 * 
	 */
	private static void sincronizarLosGsmsDeLasVpnsExistentes()
			throws SQLException {
		log.finest("Entrando");
		/*
		 * Recorre toda la lista de cuentas cvpn, busca su cuenta
		 * correspondiente en kenan y las sincroniza.
		 */
		Iterator iter = listaDeCuentasCvpn.iterator();
		CuentaCvpn cuenta;
		while (iter.hasNext()) {
			cuenta = (CuentaCvpn) iter.next();
			cuenta.sincronizaLosGsms(listaDeCuentasKenan);
		}

		log.finest("Saliendo");
	}

	/**
	 * Borra todas las cuentas de CVPN que no estén en la lista de cuentas kenan
	 * que hemos preparado anteriormente. Este es el primer paso del proceso, y
	 * consiste en borrar todas las vpns de las cuentas que sobran en cvpn
	 * porque no están en la lista de cuentas de kenan.
	 * 
	 * @throws SQLException
	 *             Si no se puede actualizar la BD de cvpn
	 * 
	 */
	private static void borrarLasCuentasQueSobranEnCvpn() throws SQLException {
		log.finest("Entra");

		Iterator iter = listaDeCuentasCvpn.iterator();
		CuentaCvpn cuenta = null;
		ArrayList listaDeCuentasBorradas = new ArrayList();
		while (iter.hasNext()) {
			cuenta = (CuentaCvpn) iter.next();
			if (cuenta.borrarSiNoEstaEnKenan(listaDeCuentasKenan)) {
				listaDeCuentasBorradas.add(cuenta);
			}
		}

		listaDeCuentasCvpn.removeAll(listaDeCuentasBorradas);

		log.finest("sale");
	}

	/**
	 * Se trae todas las cuentas de CVPN que están actualmente con una vpn
	 * activa. Es para luego saber cuales se deben borrar.
	 * 
	 * @return Lista de todas las cuenta activas en cvpn
	 * @throws SQLException
	 *             si falla el query a la BD
	 */
	private static ArrayList getListaDeCuentasCvpn() throws SQLException {
		log.finest("Entra");

		// lista donde se guardarán todas las cuenta cvpn
		listaDeCuentasCvpn = new ArrayList();

		// query para traer todas las vpn de la base de datos
		Statement stmtCvpn = conexionCvpn.createStatement();
		ResultSet resultSetCvpn = stmtCvpn
				.executeQuery("SELECT distinct id_vpn,nu_cuenta,NU_MSISDN, TX_COMPANY, ID_PLAN, ID_USER, IN_STATUS "
						+ "FROM VPN_CORP WHERE nu_cuenta is not null "
						+ "AND  in_status<>'A' "
						+ "order by id_vpn,nu_cuenta, NU_MSISDN");

		/*
		 * Como la base de datos está desfactorizada, el query se trae todos los
		 * gsms para todas las vpns, por eso hay que detectar cuando ocurre un
		 * cambio de vpn en los datos que se trae. La BD está desfactorizada,
		 * porque la entidad vpn y la entidad miembro de la vpn están juntas en
		 * una sola talba, en vez de dos tablas, que es lo natural según el tipo
		 * de dependencias funcionales.
		 */
		CuentaCvpn cuenta = null;
		CuentaCvpn cuentaPrevia = null;
		while (resultSetCvpn.next()) {
			cuenta = new CuentaCvpn();
			cuenta.setNumeroDeIdCvpn(resultSetCvpn.getString("id_vpn"));
			cuenta.setNumeroDeCuentaCvpn(resultSetCvpn.getInt("nu_cuenta"));
			// controla cuando hay cambio de cuenta
			// para saber cuando agregar a la lista y a que cuenta
			// agrega el gsm
			if (cuentaPrevia == null || !cuenta.equals(cuentaPrevia)) {
				cuenta.setNombreDeLaCompañía(resultSetCvpn
						.getString("TX_COMPANY"));
				cuenta.setIdPlan(resultSetCvpn.getInt("ID_PLAN"));
				cuenta.setIdUserSecuencial(resultSetCvpn.getString("ID_USER"));
				cuentaPrevia = cuenta;
				listaDeCuentasCvpn.add(cuenta);
			}
			cuentaPrevia.agregarGsm(resultSetCvpn);
		}

		resultSetCvpn.close();
		stmtCvpn.close();

		log.finest("Sale");

		return listaDeCuentasCvpn;
	}

	/**
	 * Se trae todas las cuetnas activas en Kenan que tiene un plan de tipo
	 * nexo. Además se trae todos los GSMs de cada una de esas cuentas
	 * 
	 * @return Lista de cuentas en Kenan
	 * @throws SQLException
	 *             si falla el acceso a la BD
	 */
	private static ArrayList getListaDeCuentasKenan() throws SQLException {
		log.finest("Entra");
		/*
		 * Traer por cuenta madre. la cuenta madre es la vpn, y los gsms de esa
		 * cuenta más los gsm de las cuentas hijas son los que conforman la vpn
		 */
		listaDeCuentasKenan = new ArrayList(20000);

		/*
		 * Se trae las cuentas válidas desde Kenan que serán utilizadas para
		 * conocer cuáles se deben quedar en cvpn y cuales hay que quitar
		 */
		String sqlCuentasKenan = SincronizadorKenanCvpn.prop
				.getProperty("cvpn.query.CuentasValidasEnKenan");

		Statement stmtKenan = conexionKenan.createStatement();
		ResultSet resultSetKenan = stmtKenan.executeQuery(sqlCuentasKenan);

		log.finest("Ejecuto query para traer cuentas");

		/*
		 * Hace un solo query para traer las cuentas con sus GSMs. por lo que
		 * hay que controlar cuando ocurre el cambio de cuenta en el recorrido
		 * del resultset. Por eso se usan estas dos variables
		 */
		CuentaKenan cuentaKenan = null;
		CuentaKenan cuentaPreviaKenan = null;
		while (resultSetKenan.next()) {
			cuentaKenan = new CuentaKenan();
			cuentaKenan.setNumeroDeCuenta(resultSetKenan.getInt("CUENTA"));
			/*
			 * Si entra, es porque ocurrió un cambio de cuenta o llegamos a una
			 * cuenta nueva en el recorrido del resultset.
			 */
			if (cuentaPreviaKenan == null
					|| !cuentaPreviaKenan.equals(cuentaKenan)) {
				// Trae el texto del plan desde kenan
				cuentaKenan.setNombreDelPlan(resultSetKenan.getString("PLAN"));
				// trae el plan desde el mapa de planes, por nombre del plan
				cuentaKenan.setNúmeroDelPlan((String) mapaDePlanes
						.get(cuentaKenan.getNombreDelPlan()));
				cuentaPreviaKenan = cuentaKenan;
				listaDeCuentasKenan.add(cuentaKenan);
			}
			cuentaPreviaKenan.getListaDeGsmsActivos().add(
					resultSetKenan.getString("GSM"));
		}
		resultSetKenan.close();
		stmtKenan.close();
		log.finest("Trajo las cuentas válidas en Kenan");

		log.finest("Sale");

		return listaDeCuentasKenan;
	}
}
