package com.megasoft.colocadorcuentas;

/**
 * Representa los GSMs que están en cvpn sin un número de cuenta asignado. Se
 * utiliza para colocarle el número de cuenta a cada gsm que esté en una vpn, ya
 * que al parecer se pueden agregar gsm a una vpn sin indicar el número de
 * cuenta; esto se puede hacer a través de la aplicación web que permite crear
 * manualmente las vpn. Si no se tiene el número de cuenta para cada gsm, el
 * resto del proceso de sincronización no será suficientemente bueno, ya que
 * podrían quedar vpn duplicadas.
 * 
 * @author cet
 * 
 */
public class GsmSinCuentaEnCvpn {
	/**
	 * Identificador de la vpn a la que pertence el gsm. Está en cvpn
	 */
	private String idDeLaVpn = null;

	/**
	 * Número de gsm en la vpn. Se toma de cvpn
	 */
	private String gsm = null;

	/**
	 * Número se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	private String idUserSecuencial = null;

	/**
	 * Número de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	private long cuenta;

	/**
	 * Número de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	public long getCuenta() {
		return cuenta;
	}

	/**
	 * Número de cuenta que debe tener asociado el gsm. Se toma de kenan y se
	 * agrega a la vpn en cvpn.
	 */
	public void setCuenta(long cuenta) {
		this.cuenta = cuenta;
	}

	/**
	 * Número de gsm en la vpn. Se toma de cvpn
	 */
	public String getGsm() {
		return gsm;
	}

	/**
	 * Número de gsm en la vpn. Se toma de cvpn
	 */
	public void setGsm(String gsm) {
		this.gsm = gsm;
	}

	/**
	 * Identificador de la vpn a la que pertence el gsm. Está en cvpn
	 */
	public String getIdDeLaVpn() {
		return idDeLaVpn;
	}

	/**
	 * Identificador de la vpn a la que pertence el gsm. Está en cvpn
	 */
	public void setIdDeLaVpn(String idDeLaVpn) {
		this.idDeLaVpn = idDeLaVpn;
	}

	/**
	 * Número se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	public String getIdUserSecuencial() {
		return idUserSecuencial;
	}

	/**
	 * Número se cuencial para cada gsm dentro de una vpn. se toma de cvpn
	 */
	public void setIdUserSecuencial(String idUserSecuencial) {
		this.idUserSecuencial = idUserSecuencial;
	}
}
