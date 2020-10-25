package com.megasoft.sincronizador;

/**
 * <p>
 * Representa un Gsm miembro de una vpn
 * </p>
 * <p>
 * Guarda el gsm y el estado del miembro dentro de la VPN.
 * </p>
 * 
 * @author cet
 *
 */
public class GsmMiembroCvpn {
	/**
	 * GSM del miembro
	 */
	private String gsm = null;

	/**
	 * <p>
	 * Es el estado del registro dentro de la VPN.
	 * </p>
	 * <p>
	 * En la tabla VPN_CORP cada registro tiene un estado. Los posibles estados
	 * que contiene esta tabla son:
	 * <li>A - Eliminado completamente. Significa que este registro ya no
	 * pertenece a ninguna VPN. En vez de borrarlo físicamente de la tabla, se
	 * coloca este estado. Cuando un registro está en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediación.
	 * <li>D - Eliminado - Pendiente en Mediación. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminación en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediación. Indica que es un registro
	 * nuevo de la VPN, pero que aún no ha sido agreagado definitivamente a la
	 * VPN a través de mediación. El registro existe en la BD de cvpn, pero aún
	 * no ha sido cargado en las tablas de mediación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserción de los
	 * datos en mediación y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediación. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediación. El dato existe en cvpn y en mediación, pero
	 * los valores no están sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y le pone estado 'O' en cvpn. La actualización la realiza en
	 * dos faces: primero borra los registros viejos de mediación (el miembro y
	 * los frecuentes), luego inserta en mediación los registros que están
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que están completamente activos en
	 * cvpn. Ningún registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediación son devueltos
	 * al estatus 'O'.
	 * </p>
	 */
	private String estado = null;
	
	/**
	 * O - Estado Activo. Son los registros que están completamente activos en
	 * cvpn. Ningún registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediación son devueltos
	 * al estatus 'O'.
	 */
	public static final String ESTADO_ACTIVO = "O";

	/**
	 * I - Ingresado - Pendiente en Mediación. Indica que es un registro nuevo
	 * de la VPN, pero que aún no ha sido agreagado definitivamente a la VPN a
	 * través de mediación. El registro existe en la BD de cvpn, pero aún no ha
	 * sido cargado en las tablas de mediación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserción de los
	 * datos en mediación y le pone estado 'O' en cvpn.
	 */
	public static final String ESTADO_INGRESADO = "I";

	/**
	 * U - Modificado - Pendiente en Mediación. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediación. El dato existe en cvpn y en mediación, pero
	 * los valores no están sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y le pone estado 'O' en cvpn. La actualización la realiza en
	 * dos faces: primero borra los registros viejos de mediación (el miembro y
	 * los frecuentes), luego inserta en mediación los registros que están
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 */
	public static final String ESTADO_MODIFICADO = "U";

	/**
	 * D - Eliminado - Pendiente en Mediación. Es un registro marcado para ser
	 * borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminación en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 */
	public static final String ESTADO_PENDIENTE_POR_ELIMINAR = "D";

	/**
	 * A - Eliminado completamente. Significa que este registro ya no pertenece
	 * a ninguna VPN. En vez de borrarlo físicamente de la tabla, se coloca este
	 * estado. Cuando un registro está en este estado, ya fue elimimado de CVPN
	 * y de las tablas de mediación.
	 */
	public static final String ESTADO_ELIMINADO = "A";

	/**
	 * <p>
	 * Es el estado del registro dentro de la VPN.
	 * </p>
	 * <p>
	 * En la tabla VPN_CORP cada registro tiene un estado. Los posibles estados
	 * que contiene esta tabla son:
	 * <li>A - Eliminado completamente. Significa que este registro ya no
	 * pertenece a ninguna VPN. En vez de borrarlo físicamente de la tabla, se
	 * coloca este estado. Cuando un registro está en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediación.
	 * <li>D - Eliminado - Pendiente en Mediación. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminación en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediación. Indica que es un registro
	 * nuevo de la VPN, pero que aún no ha sido agreagado definitivamente a la
	 * VPN a través de mediación. El registro existe en la BD de cvpn, pero aún
	 * no ha sido cargado en las tablas de mediación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserción de los
	 * datos en mediación y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediación. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediación. El dato existe en cvpn y en mediación, pero
	 * los valores no están sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y le pone estado 'O' en cvpn. La actualización la realiza en
	 * dos faces: primero borra los registros viejos de mediación (el miembro y
	 * los frecuentes), luego inserta en mediación los registros que están
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que están completamente activos en
	 * cvpn. Ningún registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediación son devueltos
	 * al estatus 'O'.
	 * </p>
	 */
	public String getEstado() {
		return estado;
	}

	/**
	 * <p>
	 * Es el estado del registro dentro de la VPN.
	 * </p>
	 * <p>
	 * En la tabla VPN_CORP cada registro tiene un estado. Los posibles estados
	 * que contiene esta tabla son:
	 * <li>A - Eliminado completamente. Significa que este registro ya no
	 * pertenece a ninguna VPN. En vez de borrarlo físicamente de la tabla, se
	 * coloca este estado. Cuando un registro está en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediación.
	 * <li>D - Eliminado - Pendiente en Mediación. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminación en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediación. Indica que es un registro
	 * nuevo de la VPN, pero que aún no ha sido agreagado definitivamente a la
	 * VPN a través de mediación. El registro existe en la BD de cvpn, pero aún
	 * no ha sido cargado en las tablas de mediación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserción de los
	 * datos en mediación y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediación. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediación. El dato existe en cvpn y en mediación, pero
	 * los valores no están sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicación. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y le pone estado 'O' en cvpn. La actualización la realiza en
	 * dos faces: primero borra los registros viejos de mediación (el miembro y
	 * los frecuentes), luego inserta en mediación los registros que están
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que están completamente activos en
	 * cvpn. Ningún registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualización en
	 * mediación y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediación son devueltos
	 * al estatus 'O'.
	 * </p>
	 */
	public void setEstado(String estado) {
		this.estado = estado;
	}

	/**
	 * GSM del miembro
	 */
	public String getGsm() {
		return gsm;
	}

	/**
	 * GSM del miembro
	 */
	public void setGsm(String gsm) {
		this.gsm = gsm;
	}

	public boolean equals(Object obj) {
		if (obj instanceof GsmMiembroCvpn) {
			GsmMiembroCvpn elOtroObjeto = (GsmMiembroCvpn) obj;
			return elOtroObjeto.getEstado().equals(this.getEstado())
					&& elOtroObjeto.getGsm().equals(this.getGsm());
		} else if (obj instanceof String) {
			String elOtroGsm = (String) obj;
			return elOtroGsm.equals(this.getGsm());
		} else {
			return false;
		}
	}

	public int hashCode() {
		return this.getGsm().hashCode();
	}

	public String toString() {
		return this.getGsm() + ":" + this.getEstado();
	}

}
