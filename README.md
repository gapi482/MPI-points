# MPI-points

-Problemi z več kot dvemi  MPI "računalniki" (mpjrun.bat -np 10 NoJson) se ustavi pri ko hoče poslat v glavnega.

-Pri več kot enem se program ne zaključi samodejno, rabim ctrl+c za Terminate batch job.
Zaradi tega rabim uporabiti MPI.finalize za vsemi operacijami (tudi sout ne dela ce ga postavim za njega).

-Clusterji večji od 5 mečejo na vsake par zagonov napako, da je nullpointerexception na 142,  ampak ko izpisujem pred tem clusters(138),
ni nikjer 0 in so podatki pravilno zračunani.
