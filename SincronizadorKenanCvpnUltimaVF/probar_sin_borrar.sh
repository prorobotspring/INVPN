# ejecuta una prueba del sincronizador
# no modifica las vpns en la base de datos, solo muestra los resultados
# de la ejecuci�n del proceso
# esta versi�n NO ejecuta NINGUA acci�n de borrado de vpns y gsms

java -cp .:classes12.jar com.megasoft.sincronizador.SincronizadorKenanCvpn sincronizador.properties prueba noborrar > resultado_sinc.txt

