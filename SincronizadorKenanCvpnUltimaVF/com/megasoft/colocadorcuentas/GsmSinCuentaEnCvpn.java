package com.megasoft.colocadorcuentas;

/**
 * Representa los GSMs que est�n en cvpn sin un n�mero de cuenta asignado. Se
 * utiliza para colocarle el n�mero de cuenta a cada gsm que est� en una vpn, ya
 * que al parecer se pueden agregar gsm a una vpn sin indicar el n�mero de
 * cuenta; esto se puede hacer a trav�s de la aplicaci�n web que permite crear
 * manualmente las vpn. Si no se tiene el n�mero de cuenta para cada gsm, el
 * resto del proceso de sincronizaci�n no ser� suficientemente bueno, ya que
 * podr�an quedar vpn duplicadas.
 * 
 * @author cet
 * 
 */
public class GsmSinCuentaEnCvpn {
	/**
	 * Identificador de la vpn a la que pertence el gsm. Est� en cvpn
	 */
	private String idDeLaVpn = null;

	/**
	 * N�mero de gsm en la vpn. Se toma de cvpn
	 */
	private String gsm = null;

	/**
	 * N�mero se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	private String idUserSecuencial = null;

	/**
	 * N�mero de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	private long cuenta;

	/**
	 * N�mero de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	public long getCuenta() {
		return cuenta;
	}

	/**
	 * N�mero de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	public void setCuenta(long cuenta) {
		this.cuenta = cuenta;
	}

	/**
	 * N�mero de gsm en la vpn. Se toma de cvpn
	 */
	public String getGsm() {
		return gsm;
	}

	/**
	 * N�mero de gsm en la vpn. Se toma de cvpn
	 */
	public void setGsm(String gsm) {
		this.gsm = gsm;
	}

	/**
	 * Identificador de la vpn a la que pertence el gsm. Est� en cvpn
	 */
	public String getIdDeLaVpn() {
		return idDeLaVpn;
	}

	/**
	 * Identificador de la vpn a la que pertence el gsm. Est� en cvpn
	 */
	public void setIdDeLaVpn(String idDeLaVpn) {
		this.idDeLaVpn = idDeLaVpn;
	}

	/**
	 * N�mero se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	public String getIdUserSecuencial() {
		return idUserSecuencial;
	}

	/**
	 * N�mero se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	public void setIdUserSecuencial(String idUserSecuencial) {
		this.idUserSecuencial = idUserSecuencial;
	}
}
