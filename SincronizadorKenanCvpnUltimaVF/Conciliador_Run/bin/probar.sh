# ejecuta una prueba del sincronizador
# no modifica las vpns en la base de datos, solo muestra los resultados
# de la ejecucion del proceso
# esta version ejecuta tambien las acciones de borrado de vpns y gsms,
#    pero solo en modo de pruebas.

java -cp classes:classes12.jar com.megasoft.sincronizador.SincronizadorKenanCvpn sincronizador.properties prueba borrar > log/resultado_sinc.txt

