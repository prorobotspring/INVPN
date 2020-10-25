package com.megasoft.sincronizador;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Representa una cuenta de la base de datos de CVPN, la cual está asociada a
 * una vpn. se utiliza para saber si se debe borrar o actualizar en cvpn
 * 
 * @author cet
 * 
 */
public class CuentaCvpn {
	/**
	 * Identificador de la VPN en la base de datos. VPN_CORP.ID_VPN
	 */
	private String numeroDeIdCvpn;

	/**
	 * número de la cuenta en Kenan a la cual pertenece esta vpn.
	 * VPN_CORP.NU_CUENTA
	 */
	private int numeroDeCuentaCvpn;

	/**
	 * Nombre de la empresa para la vpn, tal como está en VPN_CORP.TX_COMPANY
	 */
	private String nombreDeLaCompañía;

	/**
	 * Plan que tiene la vpn. VPN_CORP.ID_PLAN
	 */
	private int idPlan;

	/**
	 * Secuencial del miembro de la vpn. VPN_CORP.ID_USER
	 */
	private String idUserSecuencial;

	/**
	 * lista de gsms que pertenecen a esta vpn. se utiliza para saber cuando hay
	 * que quitar gsm de una vpn porque ya no están en la cuenta en kenan
	 */
	private ArrayList listaDeGsms = new ArrayList();

	/**
	 * Identificador de la VPN en la base de datos
	 */
	public void setNumeroDeIdCvpn(String numeroDeIdCvpn) {
		this.numeroDeIdCvpn = numeroDeIdCvpn;
	}

	/**
	 * número de la cuenta en Kenan a la cual pertenece esta vpn
	 */
	public void setNumeroDeCuentaCvpn(int numeroDeCuentaCvpn) {
		this.numeroDeCuentaCvpn = numeroDeCuentaCvpn;
	}

	/**
	 * Identificador de la VPN en la base de datos
	 */
	public String getNumeroDeIdCvpn() {
		return numeroDeIdCvpn;
	}

	/**
	 * número de la cuenta en Kenan a la cual pertenece esta vpn
	 */
	public int getNumeroDeCuentaCvpn() {
		return numeroDeCuentaCvpn;
	}

	/**
	 * Verifica si esta cuenta cvpn está en una lista de cuentas de kenan, si no
	 * está, entonces sobra en cvpn y debe ser borrada
	 * 
	 * @param listaDeCuentasKenan
	 *            lista de cuentas kenan para comparar
	 * @param listaDeModificaciones
	 *            para guardar en el log lo que se modifique
	 * @throws SQLException
	 *             si falla al borrar
	 * @return true si lo borra, false si no
	 */
	public boolean borrarSiNoEstaEnKenan(ArrayList listaDeCuentasKenan)
			throws SQLException {
		SincronizadorKenanCvpn.log.finest("Entrando");

		// si no está en la lista, la borrará
		boolean noEstáEnLaLista = true;

		// recorre la lista para buscar la cuenta
		Iterator iter = listaDeCuentasKenan.iterator();
		CuentaKenan cuenta = null;
		while (iter.hasNext()) {
			cuenta = (CuentaKenan) iter.next();
			// la consigue, se sale de la iteración
			// y no la debe borrar
			if (this.getNumeroDeCuentaCvpn() == cuenta.getNumeroDeCuenta()) {
				// borrar si la cuenta tiene un solo GSM
				noEstáEnLaLista = cuenta.getListaDeGsmsActivos().size() < 2;
				// noEstáEnLaLista = false;
				break;
			}
		}

		// borra si no está en la lista
		if (noEstáEnLaLista) {
			System.out.println("Cuenta a borrar de cvpn: " + this.toString());
			/*
			 * para dejar un registro de todo lo que se va a borrar, se realizan
			 * estos queries y se guardan los datos en la lista de
			 * modificaciones
			 */
			// frecuentes a borrar de cvpn
			PreparedStatement stmtFrecuentesABorrar = SincronizadorKenanCvpn.conexionCvpn
					.prepareStatement("SELECT ID_USER,NU_MSISDN FROM VIRTUAL_CORP WHERE ID_VPN=? ORDER BY ID_USER,NU_MSISDN");
			stmtFrecuentesABorrar.setString(1, this.getNumeroDeIdCvpn());
			ResultSet rsFrecuentesABorrar = stmtFrecuentesABorrar
					.executeQuery();
			System.out.println("Frecuentes a borrar:");
			while (rsFrecuentesABorrar.next()) {
				System.out.println("\t"
						+ rsFrecuentesABorrar.getString("ID_USER") + ","
						+ rsFrecuentesABorrar.getString("NU_MSISDN"));
			}
			rsFrecuentesABorrar.close();
			stmtFrecuentesABorrar.close();

			// miembros de la vpn
			PreparedStatement stmtMiembrosABorrar = SincronizadorKenanCvpn.conexionCvpn
					.prepareStatement("SELECT ID_VPN,TX_COMPANY,NU_MSISDN,ID_USER,ID_PLAN,NU_CUENTA FROM VPN_CORP WHERE ID_VPN=?");
			stmtMiembrosABorrar.setString(1, this.getNumeroDeIdCvpn());
			ResultSet rsMiembrosABorrar = stmtMiembrosABorrar.executeQuery();
			System.out.println("Miembros a borrar:");
			while (rsMiembrosABorrar.next()) {
				System.out.println("\t" + rsMiembrosABorrar.getString("ID_VPN")
						+ ",\"" + rsMiembrosABorrar.getString("TX_COMPANY")
						+ "\"," + rsMiembrosABorrar.getString("NU_MSISDN")
						+ "," + rsMiembrosABorrar.getString("ID_USER") + ","
						+ rsMiembrosABorrar.getInt("ID_PLAN") + ","
						+ rsMiembrosABorrar.getLong("NU_CUENTA"));
			}
			rsMiembrosABorrar.close();
			stmtMiembrosABorrar.close();

			// realiza los cambios solo si se indica por parámetro
			if (SincronizadorKenanCvpn.modificarEnBD) {
				try {
					// borrar los frecuentes de cvpn
					PreparedStatement pstmtBorrar = SincronizadorKenanCvpn.conexionCvpn
							.prepareStatement("DELETE FROM VIRTUAL_CORP WHERE ID_VPN=?");
					pstmtBorrar.setString(1, this.getNumeroDeIdCvpn());
					pstmtBorrar.executeUpdate();
					pstmtBorrar.close();

					// borra de las tablas de CVPN
					PreparedStatement stmtActualizarMiembros = SincronizadorKenanCvpn.conexionCvpn
							.prepareStatement("UPDATE VPN_CORP SET in_status='D' where id_vpn=?");
					stmtActualizarMiembros.setString(1, this
							.getNumeroDeIdCvpn());
					stmtActualizarMiembros.executeUpdate();
					stmtActualizarMiembros.close();

					// realizar commit
					SincronizadorKenanCvpn.conexionCvpn.commit();
				} catch (Exception exception) {
					SincronizadorKenanCvpn.conexionCvpn.rollback();
				}
			}
		}

		SincronizadorKenanCvpn.log.finest("Saliendo");

		return noEstáEnLaLista;
	}

	/**
	 * Compara este objeto con otro
	 */
	public boolean equals(Object obj) {
		if (obj.getClass().equals(this.getClass())) {
			CuentaCvpn comparar = (CuentaCvpn) obj;
			return comparar.getNumeroDeIdCvpn()
					.equals(this.getNumeroDeIdCvpn());
		} else {
			return false;
		}
	}

	/**
	 * Agrega un gsm a la lista de gsms de esta cuenta vpn
	 * 
	 * @param resultSetCvpn
	 *            Para obtener los valores de la BD
	 * @throws SQLException
	 *             Si no puede obtener los valores de la BD
	 */
	public void agregarGsm(ResultSet resultSetCvpn) throws SQLException {
		GsmMiembroCvpn gsm = new GsmMiembroCvpn();
		gsm.setGsm(resultSetCvpn.getString("NU_MSISDN"));
		gsm.setEstado(resultSetCvpn.getString("IN_STATUS"));
		this.listaDeGsms.add(gsm);
	}

	/**
	 * lista de gsms que pertenecen a esta vpn. se utiliza para saber cuando hay
	 * que quitar gsm de una vpn porque ya no están en la cuenta en kenan
	 */
	public ArrayList getListaDeGsms() {
		return listaDeGsms;
	}

	/**
	 * lista de gsms que pertenecen a esta vpn. se utiliza para saber cuando hay
	 * que quitar gsm de una vpn porque ya no están en la cuenta en kenan
	 */
	public void setListaDeGsms(ArrayList listaDeGsms) {
		this.listaDeGsms = listaDeGsms;
	}

	/**
	 * <p>
	 * Sincroniza los GSMs de esta vpn con los de la cuenta asociada en Kenan.
	 * </p>
	 * <p>
	 * Forma parte del tercer paso del proceso. Consiste en:
	 * <li>Busca la cuenta asociada en Kenan.</li>
	 * <li>Si un GSM de la vpn no está en la lista de gsms de Kenan, lo borra
	 * de la vpn</li>
	 * <li>Si un gsm de la cuenta de kenan no está en la lista de gsms de esta
	 * vpn, lo agrega</li>
	 * </p>
	 * 
	 * @param listaDeCuentasKenan
	 *            Lista de cuentas kenan para buscar la cuenta que corresponde
	 *            con la vpn
	 * @param modificaciones
	 *            Modificaciones para guardar en log
	 * @throws SQLException
	 *             Si falla en el acceso a la base de datos.
	 */
	public void sincronizaLosGsms(ArrayList listaDeCuentasKenan)
			throws SQLException {
		SincronizadorKenanCvpn.log.finest("Entrando");

		// buscar la cuenta que coincida en kenan
		Iterator iter = listaDeCuentasKenan.iterator();
		CuentaKenan cuentaKenan = null;
		while (iter.hasNext()) {
			cuentaKenan = (CuentaKenan) iter.next();
			if (cuentaKenan.getNumeroDeCuenta() == this.getNumeroDeCuentaCvpn()) {
				break;
			} else {
				cuentaKenan = null;
			}
		}

		// procesa si consigue la cuenta
		if (cuentaKenan != null) {
			if (SincronizadorKenanCvpn.borrarVpnsYGsms) {
				SincronizadorKenanCvpn.log
						.finest("Se eligio la opcion de borrar GSMs sobrantes.");
				this.borrarLosGsmSobrantesDeLaCuenta(cuentaKenan);
			}

			this.agregarLosGsmsQueFaltan(cuentaKenan);
		}

		SincronizadorKenanCvpn.log.finest("Sale");
	}

	/**
	 * <p>
	 * Agrega los GSMs que faltan en una VPN. Esto es porque se pueden haber
	 * agreagdo GSMs nuevos a una cuenta en Kenan, por lo que hay que agregarlos
	 * también a la VPN.
	 * </p>
	 * <p>
	 * Hay que tomar en cuenta el caso en el que ocurre un cambio de línea o
	 * cambio de número. Este caso es cuando el cliente tiene un número de GSM
	 * asignado en Kenan, y por alguna razón se le asigna otro número de GSM al
	 * mismo cliente. El número anterior queda en la tabla EXTERNAL_ID_EQUIP_MAP
	 * con tipo 31 y el INACTIVE_DATE es null.
	 * </p>
	 * 
	 * @param cuentaKenan
	 *            Cuenta en Kenan que corresponde con esta vpn
	 * @throws SQLException
	 *             si ocurre error con la BD
	 */
	private void agregarLosGsmsQueFaltan(CuentaKenan cuentaKenan)
			throws SQLException {
		/*
		 * obtener el máximo iduser para evitar duplicidad. obtiene también las
		 * fechas, ya que de otra forma en la interfaz aparecerá como si fuera
		 * otra vpn. Todo esto se utiliza al momento de insertar en cvpn y el
		 * stage
		 */
		/*PreparedStatement pstmtMáximoIdUser = SincronizadorKenanCvpn.conexionCvpn
				.prepareStatement("SELECT MAX(id_user),MAX(DT_MOD),MAX(DT_CREACION) FROM VPN_CORP WHERE ID_VPN=?");*/
		PreparedStatement pstmtMáximoIdUser = SincronizadorKenanCvpn.conexionCvpn
		.prepareStatement("SELECT MAX(id_user),sysdate,sysdate FROM VPN_CORP WHERE ID_VPN=?");
		pstmtMáximoIdUser.setString(1, this.getNumeroDeIdCvpn());
		ResultSet rsMáximoIdUser = pstmtMáximoIdUser.executeQuery();
		// aquí guardará el máximo user id
		int máximoIdUser = 0;
		Date fechaDeModificación = null;
		Date fechaDeCreación = null;
		if (rsMáximoIdUser.next()) {
			máximoIdUser = rsMáximoIdUser.getInt(1);
			fechaDeModificación = rsMáximoIdUser.getDate(2);
			fechaDeCreación = rsMáximoIdUser.getDate(3);
		} else {
			SincronizadorKenanCvpn.log
					.severe("No encuentra el máximo id y fechas para vpn: "
							+ this.getNumeroDeIdCvpn());
		}
		rsMáximoIdUser.close();
		pstmtMáximoIdUser.close();

		// TODO Cambio para ampliar el campo id_user
		/*
		 * En algún momento pedirán la ampliación del campo id_user, es decir,
		 * que en vez de tener longitud 3, tenga longitud 5. Uno de los cambios
		 * es este query, que debe pasar de tener LPAD(?,3,'0') a LPAD(?,5,'0')
		 */
		// para insertar en cvpn
		String AmpliarId_User=SincronizadorKenanCvpn.prop.getProperty("ampliacion.IdUser");
		 System.out.println("cadena para ampliar el id_user: "+ AmpliarId_User);
		
		PreparedStatement pstmtInsertaMiembro = SincronizadorKenanCvpn.conexionCvpn
				.prepareStatement("INSERT INTO CVPN.VPN_CORP ("
						+ "ID_VPN, TX_COMPANY, NU_MSISDN, "
						+ "ID_USER, IN_STATUS, ID_PLAN, "
						+ "TX_MOD_BY, DT_MOD, TX_NOTE, "
						+ "DT_CREACION, NU_CUENTA) "
						+ "VALUES (?,?,?,"+AmpliarId_User+",'I',?,?,sysdate,?,sysdate,?)");
		
		 
		
	
		
//		 para insertar en cvpn
		
	/*	PreparedStatement pstmtInsertaMiembro = SincronizadorKenanCvpn.conexionCvpn
		.prepareStatement("INSERT INTO CVPN.VPN_CORP ("
				+ "ID_VPN, TX_COMPANY, NU_MSISDN, "
				+ "ID_USER, IN_STATUS, ID_PLAN, "
				+ "TX_MOD_BY, DT_MOD, TX_NOTE, "
				+ "DT_CREACION, NU_CUENTA) "
				+ "VALUES (?,?,?,LPAD(?,5,'0'),'I',?,?,?,?,?,?)");*/

		/*
		 * luego debo agregar en la vpn los gsms que estén en kenan pero que no
		 * estén en la vpn
		 */
		Iterator iterGsmsKenan = cuentaKenan.getListaDeGsmsActivos().iterator();
		String gsmKenan;
		while (iterGsmsKenan.hasNext()) {
			boolean agregarGsmALaVpn = true;
			gsmKenan = (String) iterGsmsKenan.next();
			// va a buscar si no está en cvpn
			Iterator iterGsmsVpn = this.getListaDeGsms().iterator();
			GsmMiembroCvpn gsmVpn = null;
			while (iterGsmsVpn.hasNext()) {
				gsmVpn = (GsmMiembroCvpn) iterGsmsVpn.next();
				if (gsmVpn.equals(gsmKenan)) {
					// si lo consigue en cvpn, no debe agregarlo
					agregarGsmALaVpn = false;
					break;
				}
			}
			// si no encontro el gsm en cvpn, agregarlo
			if (agregarGsmALaVpn) {
				/*
				 * Este query sirve para saber si lo que se va a hacer con el
				 * gsm es un cambio de línea. El cambio de línea es cuando el
				 * cliente tiene un número de GSM asignado en Kenan, y por
				 * alguna razón se le asigna otro número de GSM al mismo
				 * cliente. El número anterior queda en la tabla
				 * EXTERNAL_ID_EQUIP_MAP con tipo 31 y el INACTIVE_DATE es null.
				 */
				/*
				 * En el caso en que sea un cambio de línea, en vez de borrar el
				 * registro, lo que hay que hacer es actualizar el número de GSM
				 * del registro y marcarlo para que sea sincronizado en el
				 * staging, con el estado 'U'
				 */
				PreparedStatement pstmtVerificarCambioLinea = SincronizadorKenanCvpn.conexionKenan
						.prepareStatement(SincronizadorKenanCvpn.prop
								.getProperty("cvpn.query.VerSiHayCambioDeLineaAntesDeInsertar"));
				pstmtVerificarCambioLinea.setString(1, gsmKenan);
				ResultSet rsVerificarCambioLinea = pstmtVerificarCambioLinea
						.executeQuery();
				String gsmAnteriorCambioDeLínea = null;
				if (rsVerificarCambioLinea.next()) {
					gsmAnteriorCambioDeLínea = rsVerificarCambioLinea
							.getString(1);
				}
				rsVerificarCambioLinea.close();
				pstmtVerificarCambioLinea.close();
				
				System.out.println("VALOR DEL VerSiHayCambioDeLineaAntesDeInsertar:"+ gsmAnteriorCambioDeLínea);
				
				if (gsmAnteriorCambioDeLínea != null) { 
					
					System.out.println("si hay cambio de linea");
					
					System.out.println("property: "+SincronizadorKenanCvpn.prop
							.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));
					PreparedStatement pstmtVerificarGsmEnCvpn = SincronizadorKenanCvpn.conexionCvpn
							.prepareStatement(SincronizadorKenanCvpn.prop
									.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));

					pstmtVerificarGsmEnCvpn.setString(1, gsmAnteriorCambioDeLínea);
					System.out.println("antes de ejecutar el query: "+gsmAnteriorCambioDeLínea.toString() +"-----");
					ResultSet rsVerificarCambioLineaEnCvpn = pstmtVerificarGsmEnCvpn
							.executeQuery();
					String gsmEnCvpn = null;
					if (rsVerificarCambioLineaEnCvpn.next()) {
						gsmEnCvpn = rsVerificarCambioLineaEnCvpn
								.getString(1);
					}
					rsVerificarCambioLineaEnCvpn.close();
					pstmtVerificarGsmEnCvpn.close();
					
					System.out.println("luego de consulatr en CVPN: ");
					
					if ((gsmEnCvpn!=null)){
						
						System.out.println("si existe en cvpn");
						/*
						 * En este caso es cambio de número
						 */
						System.out.println("Se va a actualizar el GSM " + gsmAnteriorCambioDeLínea
								+ " para la vpn: " + this.getNumeroDeIdCvpn()
								+ " con el nuevo GSM: " + gsmKenan);

						// modifica solo si hay commit
						if (SincronizadorKenanCvpn.modificarEnBD) {
							try {
								/*
								 * actualizar el registro en CVPN para que luego sea
								 * sincronizado en el staging
								 */
								PreparedStatement stmtActualizarMiembros = SincronizadorKenanCvpn.conexionCvpn
										.prepareStatement(SincronizadorKenanCvpn.prop
												.getProperty("cvpn.update.ActualizarParaCambioDeLinea"));
								stmtActualizarMiembros.setString(1, gsmKenan);
								stmtActualizarMiembros.setString(2, this
										.getNumeroDeIdCvpn());
								stmtActualizarMiembros.setString(3,
										gsmAnteriorCambioDeLínea);
								stmtActualizarMiembros.executeUpdate();
								System.out.println("se actualizo:  gsmKenan: "+ gsmKenan);
								stmtActualizarMiembros.close();

								// commit de todos los cambios
								SincronizadorKenanCvpn.conexionCvpn.commit();
							} catch (Exception exception) {
								System.out.println("ocurrio un error al actualizar el gsm "+ gsmKenan+ ".error: "+ exception.getMessage());
								SincronizadorKenanCvpn.conexionCvpn.rollback();
							}
						}
						
						
					}else{
						
						System.out.println("property: "+SincronizadorKenanCvpn.prop
								.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));
						PreparedStatement pstmtVerificarGsmEnCvpn1 = SincronizadorKenanCvpn.conexionCvpn
								.prepareStatement(SincronizadorKenanCvpn.prop
										.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));

						pstmtVerificarGsmEnCvpn1.setString(1, gsmKenan);
						System.out.println("pregunta si el nuevo existe en CVPN cambio de linea: "+gsmKenan.toString() +"-----");
						ResultSet rsVerificarCambioLineaEnCvpn1 = pstmtVerificarGsmEnCvpn1
								.executeQuery();
						String gsmEnCvpn1 = null;
						if (rsVerificarCambioLineaEnCvpn1.next()) {
							gsmEnCvpn1 = rsVerificarCambioLineaEnCvpn1
									.getString(1);
						}
						rsVerificarCambioLineaEnCvpn1.close();
						pstmtVerificarGsmEnCvpn1.close();
						
						if (gsmEnCvpn1!=null){
							
							System.out.println("ya existe en cvpn, no se hace el cambio mde linea-");
							
							
							
						}else{
							
							System.out.println("Agregando GSM a la cuenta cuando es cambio de linea"
									+ this.getNumeroDeCuentaCvpn() + "," + gsmKenan);

							// inserta en base de datos solo si se le indica al proceso
							if (SincronizadorKenanCvpn.modificarEnBD) {
								try {
									// insertar el miembro
									pstmtInsertaMiembro.setString(1, this
											.getNumeroDeIdCvpn());
									pstmtInsertaMiembro.setString(2, this
											.getNombreDeLaCompañía());
									pstmtInsertaMiembro.setString(3, gsmKenan);
									pstmtInsertaMiembro.setInt(4, ++máximoIdUser);
									pstmtInsertaMiembro.setInt(5, this.getIdPlan());
									pstmtInsertaMiembro.setString(6,SincronizadorKenanCvpn.usuarioQueCambiaLasVpns);
									//pstmtInsertaMiembro.setDate(7, fechaDeModificación);
									pstmtInsertaMiembro.setString(7,SincronizadorKenanCvpn.notaParaIserciónDeVpn);
									//pstmtInsertaMiembro.setDate(9, fechaDeCreación);
									pstmtInsertaMiembro.setLong(8, this.getNumeroDeCuentaCvpn());
									pstmtInsertaMiembro.executeUpdate();

									SincronizadorKenanCvpn.conexionCvpn.commit();
								} catch (SQLException e) {
									SincronizadorKenanCvpn.log
											.severe("Falló insertando un GSM a la VPN");
									SincronizadorKenanCvpn.log.throwing("CuentaCvpn",
											"sincronizaLosGsms", e);
									SincronizadorKenanCvpn.conexionCvpn.rollback();
								}
							}
									
						}
						
						
						}
						
						
					
				}else {
					
					System.out.println("property: "+SincronizadorKenanCvpn.prop
							.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));
					PreparedStatement pstmtVerificarGsmEnCvpn1 = SincronizadorKenanCvpn.conexionCvpn
							.prepareStatement(SincronizadorKenanCvpn.prop
									.getProperty("cvpn.query.BuscarEnCvpnCambioDeLinea"));

					pstmtVerificarGsmEnCvpn1.setString(1, gsmKenan);
					System.out.println("pregunta si el nuevo existe en CVPN cambio de linea: "+gsmKenan.toString() +"-----");
					ResultSet rsVerificarCambioLineaEnCvpn1 = pstmtVerificarGsmEnCvpn1
							.executeQuery();
					String gsmEnCvpn2 = null;
					if (rsVerificarCambioLineaEnCvpn1.next()) {
						gsmEnCvpn2 = rsVerificarCambioLineaEnCvpn1
								.getString(1);
					}
					rsVerificarCambioLineaEnCvpn1.close();
					pstmtVerificarGsmEnCvpn1.close();

					
					if (gsmEnCvpn2!=null){
						
						System.out.println("ya existe en cvpn, no se agrega cuando no es cambio de linea");
						
						
						
					}else{
						
						System.out.println("Agregando GSM a la cuenta cuando no es cambio de linea"
								+ this.getNumeroDeCuentaCvpn() + "," + gsmKenan);

						// inserta en base de datos solo si se le indica al proceso
						if (SincronizadorKenanCvpn.modificarEnBD) {
							try {
								// insertar el miembro
								pstmtInsertaMiembro.setString(1, this
										.getNumeroDeIdCvpn());
								pstmtInsertaMiembro.setString(2, this
										.getNombreDeLaCompañía());
								pstmtInsertaMiembro.setString(3, gsmKenan);
								pstmtInsertaMiembro.setInt(4, ++máximoIdUser);
								pstmtInsertaMiembro.setInt(5, this.getIdPlan());
								pstmtInsertaMiembro.setString(6,SincronizadorKenanCvpn.usuarioQueCambiaLasVpns);
								//pstmtInsertaMiembro.setDate(7, fechaDeModificación);
								pstmtInsertaMiembro.setString(7,SincronizadorKenanCvpn.notaParaIserciónDeVpn);
								//pstmtInsertaMiembro.setDate(9, fechaDeCreación);
								pstmtInsertaMiembro.setLong(8, this.getNumeroDeCuentaCvpn());
								pstmtInsertaMiembro.executeUpdate();

								SincronizadorKenanCvpn.conexionCvpn.commit();
							} catch (SQLException e) {
								SincronizadorKenanCvpn.log
										.severe("Falló insertando un GSM a la VPN");
								SincronizadorKenanCvpn.log.throwing("CuentaCvpn",
										"sincronizaLosGsms", e);
								SincronizadorKenanCvpn.conexionCvpn.rollback();
							}
						}
					}
					
						
					
						
					}
					
					
					
					
					
					
					
					
					
					
					
					
			
			}
		}//fin del ciclo
		pstmtInsertaMiembro.close();
	}

	/**
	 * <p>
	 * Borra los GSMs que estén sobrando en una vpn. Esto es porque los GSMs que
	 * están en cvpn, pero que no están en la cuenta en kenan, deben ser
	 * quitados de la vpn.
	 * </p>
	 * <p>
	 * Hay que tomar en cuenta el caso en el que ocurre un cambio de línea o
	 * cambio de número. Este caso es cuando el cliente tiene un número de GSM
	 * asignado en Kenan, y por alguna razón se le asigna otro número de GSM al
	 * mismo cliente. El número anterior queda en la tabla EXTERNAL_ID_EQUIP_MAP
	 * con tipo 31 y el INACTIVE_DATE es null.
	 * </p>
	 * 
	 * @param cuentaKenan
	 *            Cuenta en Kenan que corresponde con esta vpn
	 * @throws SQLException
	 *             si ocurre error con la BD
	 */
	private void borrarLosGsmSobrantesDeLaCuenta(CuentaKenan cuentaKenan)
			throws SQLException {
		/*
		 * primero debe borrar los gsms que están en la vpn y que no están en
		 * kenan
		 */
		Iterator iterGsmsKenan;
		Iterator iterGsmsVpn = this.getListaDeGsms().iterator();
		GsmMiembroCvpn gsmVpn;
		ArrayList gsmsBorrados = new ArrayList();
		while (iterGsmsVpn.hasNext()) {
			boolean borrarGsmDeLaVpn = true;
			gsmVpn = (GsmMiembroCvpn) iterGsmsVpn.next();
			// va a buscar cada gsm en la lista de gsms de la cuenta kenan
			iterGsmsKenan = cuentaKenan.getListaDeGsmsActivos().iterator();
			String gsmKenan = null;
			while (iterGsmsKenan.hasNext()) {
				gsmKenan = (String) iterGsmsKenan.next();
				if (gsmVpn.equals(gsmKenan)) {
					// si encuentra el gsm en kenan, no lo debe borrar
					borrarGsmDeLaVpn = false;
					break;
				}
			}
			// si no consiguió el gsm en kenan, debe borrarlo de cvpn
			if (borrarGsmDeLaVpn) {
				/*
				 * Este query sirve para saber si lo que se va a hacer con el
				 * gsm es un cambio de línea. El cambio de línea es cuando el
				 * cliente tiene un número de GSM asignado en Kenan, y por
				 * alguna razón se le asigna otro número de GSM al mismo
				 * cliente. El número anterior queda en la tabla
				 * EXTERNAL_ID_EQUIP_MAP con tipo 31 y el INACTIVE_DATE es null.
				 */
				/*
				 * En el caso en que sea un cambio de línea, en vez de borrar el
				 * registro, lo que hay que hacer es actualizar el número de GSM
				 * del registro y marcarlo para que sea sincronizado en el
				 * staging, con el estado 'U'
				 */
				PreparedStatement pstmtVerificarCambioLinea = SincronizadorKenanCvpn.conexionKenan
						.prepareStatement(SincronizadorKenanCvpn.prop
								.getProperty("cvpn.query.VerSiHayCambioDeLineaAntesDeBorrar"));
				pstmtVerificarCambioLinea.setString(1, gsmVpn.getGsm());
				ResultSet rsVerificarCambioLinea = pstmtVerificarCambioLinea
						.executeQuery();
				String gsmNuevoParaCambioDeLínea = null;
				if (rsVerificarCambioLinea.next()) {
					gsmNuevoParaCambioDeLínea = rsVerificarCambioLinea
							.getString(1);
				}
				rsVerificarCambioLinea.close();
				pstmtVerificarCambioLinea.close();

				if (gsmNuevoParaCambioDeLínea != null) {
					/*
					 * En este caso es cambio de número
					 */
					System.out
							.println("Se va a actualizar el GSM " + gsmVpn
									+ " para la vpn: "
									+ this.getNumeroDeIdCvpn()
									+ " con el nuevo GSM: "
									+ gsmNuevoParaCambioDeLínea);

					// modifica solo si hay commit
					if (SincronizadorKenanCvpn.modificarEnBD) {
						try {
							/*
							 * actualizar el registro en CVPN para que luego sea
							 * sincronizado en el staging
							 */
							PreparedStatement stmtActualizarMiembros = SincronizadorKenanCvpn.conexionCvpn
									.prepareStatement(SincronizadorKenanCvpn.prop
											.getProperty("cvpn.update.ActualizarParaCambioDeLinea"));
							stmtActualizarMiembros.setString(1,
									gsmNuevoParaCambioDeLínea);
							stmtActualizarMiembros.setString(2, this
									.getNumeroDeIdCvpn());
							stmtActualizarMiembros
									.setString(3, gsmVpn.getGsm());
							stmtActualizarMiembros.executeUpdate();
							stmtActualizarMiembros.close();

							// commit de todos los cambios
							SincronizadorKenanCvpn.conexionCvpn.commit();
						} catch (Exception exception) {
							SincronizadorKenanCvpn.conexionCvpn.rollback();
						}
					}
				} else {
					/*
					 * En este caso es un borrado normal
					 */
					gsmsBorrados.add(gsmVpn);
					System.out.print("Se van a borrar GSMs para la cuenta: ");
					System.out.println(this.getNumeroDeIdCvpn() + "," + gsmVpn);

					/*
					 * Para borrar todo de la BD hay que tener el secuencial del
					 * ID_USER en la tabla VPN_CORP. Esto es para poder borrar
					 * sus frecuentes
					 */
					PreparedStatement stmtIdUserSecuencial = SincronizadorKenanCvpn.conexionCvpn
							.prepareStatement("SELECT ID_USER FROM VPN_CORP WHERE ID_VPN=? AND NU_MSISDN=? AND IN_STATUS<>'D'");
					stmtIdUserSecuencial.setString(1, this.getNumeroDeIdCvpn());
					stmtIdUserSecuencial.setString(2, gsmVpn.getGsm());
					ResultSet rsIdUserSecuencial = stmtIdUserSecuencial
							.executeQuery();
					if (rsIdUserSecuencial.next()) {
						String idUserSecuencial = rsIdUserSecuencial
								.getString("ID_USER");
						rsIdUserSecuencial.close();
						stmtIdUserSecuencial.close();

						/*
						 * para dejar un registro de todo lo que se va a borrar,
						 * se realizan estos queries y se guardan los datos en
						 * la lista de modificaciones
						 */
						// frecuentes a borrar de cvpn
						PreparedStatement stmtFrecuentesABorrar = SincronizadorKenanCvpn.conexionCvpn
								.prepareStatement("SELECT ID_USER,NU_MSISDN FROM VIRTUAL_CORP WHERE ID_VPN=? AND ID_USER=? ORDER BY ID_USER,NU_MSISDN");
						stmtFrecuentesABorrar.setString(1, this
								.getNumeroDeIdCvpn());
						stmtFrecuentesABorrar.setString(2, idUserSecuencial);
						ResultSet rsFrecuentesABorrar = stmtFrecuentesABorrar
								.executeQuery();
						System.out.println("Frecuentes borrados:");
						while (rsFrecuentesABorrar.next()) {
							System.out.println("\t"
									+ rsFrecuentesABorrar.getString("ID_USER")
									+ ","
									+ rsFrecuentesABorrar
											.getString("NU_MSISDN"));
						}
						rsFrecuentesABorrar.close();
						stmtFrecuentesABorrar.close();

						// miembros de la vpn
						PreparedStatement stmtMiembrosABorrar = SincronizadorKenanCvpn.conexionCvpn
								.prepareStatement("SELECT ID_VPN,TX_COMPANY,NU_MSISDN,ID_USER,ID_PLAN,NU_CUENTA FROM VPN_CORP WHERE ID_VPN=? AND NU_MSISDN=?");
						stmtMiembrosABorrar.setString(1, this
								.getNumeroDeIdCvpn());
						stmtMiembrosABorrar.setString(2, gsmVpn.getGsm());
						ResultSet rsMiembrosABorrar = stmtMiembrosABorrar
								.executeQuery();
						System.out.println("Miembros a borrar:");
						while (rsMiembrosABorrar.next()) {
							System.out.println("\t"
									+ rsMiembrosABorrar.getString("ID_VPN")
									+ ",\""
									+ rsMiembrosABorrar.getString("TX_COMPANY")
									+ "\","
									+ rsMiembrosABorrar.getString("NU_MSISDN")
									+ ","
									+ rsMiembrosABorrar.getString("ID_USER")
									+ "," + rsMiembrosABorrar.getInt("ID_PLAN")
									+ ","
									+ rsMiembrosABorrar.getLong("NU_CUENTA"));
						}
						rsMiembrosABorrar.close();
						stmtMiembrosABorrar.close();

						// borrar solo si se indica
						if (SincronizadorKenanCvpn.modificarEnBD) {
							try {
								// borrar los frecuentes de cvpn
								PreparedStatement pstmtBorrar = SincronizadorKenanCvpn.conexionCvpn
										.prepareStatement("DELETE FROM VIRTUAL_CORP WHERE ID_VPN=? AND ID_USER=?");
								pstmtBorrar.setString(1, this
										.getNumeroDeIdCvpn());
								pstmtBorrar.setString(2, idUserSecuencial);
								pstmtBorrar.executeUpdate();
								pstmtBorrar.close();

								// borra de las tablas de CVPN poniendo estado
								// 'D'
								PreparedStatement stmtActualizarMiembros = SincronizadorKenanCvpn.conexionCvpn
										.prepareStatement("UPDATE VPN_CORP SET in_status='D' where id_vpn=? AND NU_MSISDN=?");
								stmtActualizarMiembros.setString(1, this
										.getNumeroDeIdCvpn());
								stmtActualizarMiembros.setString(2, gsmVpn
										.getGsm());
								stmtActualizarMiembros.executeUpdate();
								stmtActualizarMiembros.close();

								// commit de todos los cambios
								SincronizadorKenanCvpn.conexionCvpn.commit();
							} catch (Exception exception) {
								SincronizadorKenanCvpn.conexionCvpn.rollback();
							}
						}
					} else {
						// cerrar los objetos jdbc aún si ocurriera un error
						rsIdUserSecuencial.close();
						stmtIdUserSecuencial.close();
						SincronizadorKenanCvpn.log
								.severe("No pudo traer el secuencial ID_USER para la vpn: "
										+ this.getNumeroDeIdCvpn());
						SincronizadorKenanCvpn.log
								.severe("No se pudo borrar el miembro.");
					}
				}
			}
		}
		// quitar los gsms borrados de la lista de gsms de la vpn
		this.listaDeGsms.removeAll(gsmsBorrados);
	}

	/**
	 * Plan que tiene la vpn. VPN_CORP.ID_PLAN
	 */
	public int getIdPlan() {
		return idPlan;
	}

	/**
	 * Plan que tiene la vpn. VPN_CORP.ID_PLAN
	 */
	public void setIdPlan(int idPlan) {
		this.idPlan = idPlan;
	}

	/**
	 * Secuencial del miembro de la vpn. VPN_CORP.ID_USER
	 */
	public String getIdUserSecuencial() {
		return idUserSecuencial;
	}

	/**
	 * Secuencial del miembro de la vpn. VPN_CORP.ID_USER
	 */
	public void setIdUserSecuencial(String idUserSecuencial) {
		this.idUserSecuencial = idUserSecuencial;
	}

	/**
	 * Nombre de la empresa para la vpn, tal como está en VPN_CORP.TX_COMPANY
	 */
	public String getNombreDeLaCompañía() {
		return nombreDeLaCompañía;
	}

	/**
	 * Nombre de la empresa para la vpn, tal como está en VPN_CORP.TX_COMPANY
	 */
	public void setNombreDeLaCompañía(String nombreDeLaCompañía) {
		this.nombreDeLaCompañía = nombreDeLaCompañía;
	}

	public int hashCode() {
		return this.numeroDeCuentaCvpn;
	}

	public String toString() {
		return "VPN: " + this.getNumeroDeIdCvpn() + ":" + this.getIdPlan()
				+ ":" + this.getNumeroDeCuentaCvpn() + ":"
				+ this.getNombreDeLaCompañía();
	}

}
