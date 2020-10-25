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
	 * pertenece a ninguna VPN. En vez de borrarlo f�sicamente de la tabla, se
	 * coloca este estado. Cuando un registro est� en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediaci�n.
	 * <li>D - Eliminado - Pendiente en Mediaci�n. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminaci�n en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediaci�n. Indica que es un registro
	 * nuevo de la VPN, pero que a�n no ha sido agreagado definitivamente a la
	 * VPN a trav�s de mediaci�n. El registro existe en la BD de cvpn, pero a�n
	 * no ha sido cargado en las tablas de mediaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserci�n de los
	 * datos en mediaci�n y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediaci�n. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediaci�n. El dato existe en cvpn y en mediaci�n, pero
	 * los valores no est�n sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y le pone estado 'O' en cvpn. La actualizaci�n la realiza en
	 * dos faces: primero borra los registros viejos de mediaci�n (el miembro y
	 * los frecuentes), luego inserta en mediaci�n los registros que est�n
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que est�n completamente activos en
	 * cvpn. Ning�n registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediaci�n son devueltos
	 * al estatus 'O'.
	 * </p>
	 */
	private String estado = null;
	
	/**
	 * O - Estado Activo. Son los registros que est�n completamente activos en
	 * cvpn. Ning�n registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediaci�n son devueltos
	 * al estatus 'O'.
	 */
	public static final String ESTADO_ACTIVO = "O";

	/**
	 * I - Ingresado - Pendiente en Mediaci�n. Indica que es un registro nuevo
	 * de la VPN, pero que a�n no ha sido agreagado definitivamente a la VPN a
	 * trav�s de mediaci�n. El registro existe en la BD de cvpn, pero a�n no ha
	 * sido cargado en las tablas de mediaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserci�n de los
	 * datos en mediaci�n y le pone estado 'O' en cvpn.
	 */
	public static final String ESTADO_INGRESADO = "I";

	/**
	 * U - Modificado - Pendiente en Mediaci�n. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediaci�n. El dato existe en cvpn y en mediaci�n, pero
	 * los valores no est�n sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y le pone estado 'O' en cvpn. La actualizaci�n la realiza en
	 * dos faces: primero borra los registros viejos de mediaci�n (el miembro y
	 * los frecuentes), luego inserta en mediaci�n los registros que est�n
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 */
	public static final String ESTADO_MODIFICADO = "U";

	/**
	 * D - Eliminado - Pendiente en Mediaci�n. Es un registro marcado para ser
	 * borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminaci�n en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 */
	public static final String ESTADO_PENDIENTE_POR_ELIMINAR = "D";

	/**
	 * A - Eliminado completamente. Significa que este registro ya no pertenece
	 * a ninguna VPN. En vez de borrarlo f�sicamente de la tabla, se coloca este
	 * estado. Cuando un registro est� en este estado, ya fue elimimado de CVPN
	 * y de las tablas de mediaci�n.
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
	 * pertenece a ninguna VPN. En vez de borrarlo f�sicamente de la tabla, se
	 * coloca este estado. Cuando un registro est� en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediaci�n.
	 * <li>D - Eliminado - Pendiente en Mediaci�n. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminaci�n en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediaci�n. Indica que es un registro
	 * nuevo de la VPN, pero que a�n no ha sido agreagado definitivamente a la
	 * VPN a trav�s de mediaci�n. El registro existe en la BD de cvpn, pero a�n
	 * no ha sido cargado en las tablas de mediaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserci�n de los
	 * datos en mediaci�n y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediaci�n. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediaci�n. El dato existe en cvpn y en mediaci�n, pero
	 * los valores no est�n sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y le pone estado 'O' en cvpn. La actualizaci�n la realiza en
	 * dos faces: primero borra los registros viejos de mediaci�n (el miembro y
	 * los frecuentes), luego inserta en mediaci�n los registros que est�n
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que est�n completamente activos en
	 * cvpn. Ning�n registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediaci�n son devueltos
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
	 * pertenece a ninguna VPN. En vez de borrarlo f�sicamente de la tabla, se
	 * coloca este estado. Cuando un registro est� en este estado, ya fue
	 * elimimado de CVPN y de las tablas de mediaci�n.
	 * <li>D - Eliminado - Pendiente en Mediaci�n. Es un registro marcado para
	 * ser borrado de una VPN. Los datos existen en las tablas de cvpn, pero no
	 * existen en las tablas de medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la eliminaci�n en las
	 * tablas de staging y luego le cambia el estado del valor actual 'D' hacia
	 * el valor definitivo 'A'.
	 * <li>I - Ingresado - Pendiente en Mediaci�n. Indica que es un registro
	 * nuevo de la VPN, pero que a�n no ha sido agreagado definitivamente a la
	 * VPN a trav�s de mediaci�n. El registro existe en la BD de cvpn, pero a�n
	 * no ha sido cargado en las tablas de mediaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la inserci�n de los
	 * datos en mediaci�n y le pone estado 'O' en cvpn.
	 * <li>U - Modificado - Pendiente en Mediaci�n. Indica que es un registro
	 * antiguo que ya pertenece a una VPN, pero que ha sido modificado y debe
	 * actualizarce en mediaci�n. El dato existe en cvpn y en mediaci�n, pero
	 * los valores no est�n sincronizados correctamente, ya que hubo cambios en
	 * cvpn que deben ser reflejados en medicaci�n. El stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y le pone estado 'O' en cvpn. La actualizaci�n la realiza en
	 * dos faces: primero borra los registros viejos de mediaci�n (el miembro y
	 * los frecuentes), luego inserta en mediaci�n los registros que est�n
	 * actualmente en cvpn (el miembro y los frecuentes actuales).
	 * <li>O - Activo. Son los registros que est�n completamente activos en
	 * cvpn. Ning�n registro entra directamente con este estado, sino que pasa
	 * por el estado 'I' y luego el stored procedure
	 * 'Actualizar_Vpn_Corporativa' se encarga de realizar la actualizaci�n en
	 * mediaci�n y ponerle el estado 'O' en cvpn. Igualmente pasa con los
	 * registros que han sido actualizados, se les coloca estatus 'U'
	 * inicialmente, y una vez que son actualizados en mediaci�n son devueltos
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
