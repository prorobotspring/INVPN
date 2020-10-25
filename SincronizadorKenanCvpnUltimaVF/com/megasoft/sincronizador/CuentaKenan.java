package com.megasoft.sincronizador;

import java.util.ArrayList;

/**
 * <p>
 * Representa una cuenta en Kenan que tiene un plan de CVPN.
 * </p>
 * <p>
 * Se utiliza para saber cuales cuentas deberían estar realmente en CVPN y
 * también para sincronizar los GSMs de las cuentas de Kenan con las VPNs
 * </p>
 * 
 * @author cet
 * 
 */
public class CuentaKenan {
	/**
	 * Número de la cuenta en Kenan (CMF.ACCOUNT_NO)
	 */
	private int numeroDeCuenta;

	/**
	 * Número del plan (paquete) en Kenan que tiene esta cuenta
	 * (CMF_PACKAGES.PACKAGE_ID)
	 */
	private int numeroDePaquete;

	/**
	 * Nombre de la empresa. Se utiliza para insertar en cvpn una nueva vpn.
	 */
	private String nombreDeLaEmpresa = null;

	/**
	 * Nombre del plan tal como viene de Kenan. Se usa para luego obtener el
	 * número del plan. El nonmbre es lo único que puedo obtener en kenan, para
	 * luego obtener el número desde los planes de cvpn.
	 */
	private String nombreDelPlan = null;

	/**
	 * Este es el número del plan según la tabla VPN_CORP_CONFIG de cvpn. Se
	 * obtiene comparando el nombre del plan de cvpn con el de Kenan.
	 */
	private String númeroDelPlan = null;

	/**
	 * Son los GSMs que tiene activos una cuenta de Kenan
	 */
	private ArrayList listaDeGsmsActivos = new ArrayList(10);

	/**
	 * Número de la cuenta en Kenan (CMF.ACCOUNT_NO)
	 */
	public void setNumeroDeCuenta(int numeroDeCuenta) {
		this.numeroDeCuenta = numeroDeCuenta;
	}

	/**
	 * Número del plan (paquete) en Kenan que tiene esta cuenta
	 * (CMF_PACKAGES.PACKAGE_ID)
	 */
	public void setNumeroDePaquete(int numeroDePaquete) {
		this.numeroDePaquete = numeroDePaquete;
	}

	/**
	 * Número de la cuenta en Kenan (CMF.ACCOUNT_NO)
	 */
	public int getNumeroDeCuenta() {
		return numeroDeCuenta;
	}

	/**
	 * Número del plan (paquete) en Kenan que tiene esta cuenta
	 * (CMF_PACKAGES.PACKAGE_ID)
	 */
	public int getNumeroDePaquete() {
		return numeroDePaquete;
	}

	/**
	 * Son los GSMs que tiene activos una cuenta de Kenan. 
	 */
	public ArrayList getListaDeGsmsActivos() {
		return listaDeGsmsActivos;
	}

	/**
	 * Son los GSMs que tiene activos una cuenta de Kenan
	 */
	public void setListaDeGsmsActivos(ArrayList listaDeGsmsActivos) {
		this.listaDeGsmsActivos = listaDeGsmsActivos;
	}

	/**
	 * Nombre de la empresa. Se utiliza para insertar en cvpn una nueva vpn.
	 */
	public String getNombreDeLaEmpresa() {
		return nombreDeLaEmpresa;
	}

	/**
	 * Nombre de la empresa. Se utiliza para insertar en cvpn una nueva vpn.
	 */
	public void setNombreDeLaEmpresa(String nombreDeLaEmpresa) {
		this.nombreDeLaEmpresa = nombreDeLaEmpresa;
	}

	/**
	 * Nombre del plan tal como viene de Kenan. Se usa para luego obtener el
	 * número del plan. El nonmbre es lo único que puedo obtener en kenan, para
	 * luego obtener el número desde los planes de cvpn.
	 */
	public String getNombreDelPlan() {
		return nombreDelPlan;
	}

	/**
	 * Nombre del plan tal como viene de Kenan. Se usa para luego obtener el
	 * número del plan. El nonmbre es lo único que puedo obtener en kenan, para
	 * luego obtener el número desde los planes de cvpn.
	 */
	public void setNombreDelPlan(String nombreDelPlan) {
		this.nombreDelPlan = nombreDelPlan;
	}

	/**
	 * Este es el número del plan según la tabla VPN_CORP_CONFIG de cvpn. Se
	 * obtiene comparando el nombre del plan de cvpn con el de Kenan.
	 */
	public String getNúmeroDelPlan() {
		return númeroDelPlan;
	}

	/**
	 * Este es el número del plan según la tabla VPN_CORP_CONFIG de cvpn. Se
	 * obtiene comparando el nombre del plan de cvpn con el de Kenan.
	 */
	public void setNúmeroDelPlan(String númeroDelPlan) {
		this.númeroDelPlan = númeroDelPlan;
	}

	public boolean equals(Object obj) {
		if (obj instanceof CuentaKenan) {
			CuentaKenan elOtroObj = (CuentaKenan) obj;
			return elOtroObj.getNumeroDeCuenta() == this.getNumeroDeCuenta();
		} else {
			return false;
		}
	}

	public String toString() {
		return this.getNumeroDeCuenta() + ":" + this.getNombreDeLaEmpresa()
				+ ":" + this.getNúmeroDelPlan() + ":" + this.getNombreDelPlan();
	}

}
