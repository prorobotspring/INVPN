# ejecuta el sincronizador en modo commit
# modifica las vpns en la base de datos y luego sincroniza
# en el staging usando el stored procedure de sincronizacion
# esta versi�n NO ejecuta NINGUA acci�n de borrado de vpns y gsms

java -cp .:classes12.jar com.megasoft.sincronizador.SincronizadorKenanCvpn sincronizador.properties commit noborrar > resultado_sinc.txt

