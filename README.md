# MPI-points


-Pri več kot enem se program ne zaključi samodejno, rabim ctrl+c za Terminate batch job.
Zaradi tega rabim uporabiti MPI.finalize za vsemi operacijami (tudi sout ne dela ce ga postavim za njega).

-Problemi z velikostjo pri scatte rin gather, saj če povečam št. procesov (mpjrun 2--> mpjrun 6) isto kodo mi napiše indexoutofbounce 17 za clusterje velikosti 5.
Nevem zakaj bi št. procesov vplivalo na podatke, če vsem pošljem isti začetni cluster in potem vsak določi koordinate za svoj chunk vseh točk (data).

