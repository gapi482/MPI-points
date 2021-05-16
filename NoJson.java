import mpi.MPI;

import java.util.Random;

public class NoJson {

    public static void main(String[] args) {
        //INIT
        double t0 = System.currentTimeMillis();
        MPI.Init(args);
        Random rand = new Random();
        int cluster = 5;
        int tras = 12932;
        float[][] clusters = new float[cluster][5]; //old, old, new, new, št točk
        float[][] data = new float[tras][2]; //random točke
        boolean changed;
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        float[][] clusterget = new float[cluster][5];
        int chunki = tras / size;
        int maxi;
        int[] trash = new int[tras]; //pripada
        //////////////////////////////////////////////////////////////
        for (int i = 0; i < tras; i++) {
            data[i][0] = (float) (rand.nextFloat() * (55.1 - 46) + 46); //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
            data[i][1] = (float) (rand.nextFloat() * (17.5 - 5.5) + 5.5); //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
        }
        if (me == 0) {
            //generiraj podatke
            for (int i = 0; i < clusters.length; i++) {
                clusters[i][0] = (float) (rand.nextFloat() * (55.1 - 46) + 46); //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
                clusters[i][1] = (float) (rand.nextFloat() * (17.5 - 5.5) + 5.5); //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
                clusters[i][2] = 0; //new
                clusters[i][3] = 0;
                clusters[i][4] = 0;
            }
        }
        //DELAJ
        while (true) {
            MPI.COMM_WORLD.Bcast(clusters, 0, clusters.length, MPI.OBJECT, 0);
            MPI.COMM_WORLD.Barrier();
            if (size != me + 1)
                maxi = me * chunki + chunki;
            else
                maxi = data.length;
            // Izracunaj
            for (int j = me*chunki; j < maxi; j++) {
                float mini = (float) Math.hypot(Math.abs(clusters[0][0] - data[j][0]), Math.abs(clusters[0][1] - data[j][1]));//prvi cluster

                for (int k = 0; k < clusters.length; k++) {
                    if ((float) Math.hypot(Math.abs(clusters[k][0] - data[j][0]), Math.abs(clusters[k][1] - data[j][1])) <= mini) { //če je drug cluster bližji zamenjaj
                        mini = (float) Math.hypot(Math.abs(clusters[k][0] - data[j][0]), Math.abs(clusters[k][1] - data[j][1]));
                        trash[j] = k; //katermu clusterju pripada
                    }
                }
                clusters[trash[j]][4]++; //št el. na clusterju
                clusters[trash[j]][2] += data[j][0]; //seštevamo sproti v new
                clusters[trash[j]][3] += data[j][1]; //seštevamo sproti v new
            }
            MPI.COMM_WORLD.Barrier();
            if (me != 0){
                MPI.COMM_WORLD.Send(clusters,0,clusters.length,MPI.OBJECT,0,MPI.ANY_TAG);
            }


            //prejmi
            //MPI.COMM_WORLD.Gather(clusters, 0, clusters.length / size, MPI.OBJECT, clusterget, 0, clusters.length / size, MPI.OBJECT, 0);

            if (me == 0) {  // MPI.COMM_WORLD.Barrier();
                for (int i = 1; i < size; i++) {
                    MPI.COMM_WORLD.Recv(clusterget,0,clusters.length,MPI.OBJECT,i,MPI.ANY_TAG);
                    for (int j = 0; j < clusters.length; j++) {
                        //System.out.println(clusterget[j][4]+" tock "+clusters[j][4]);
                        clusters[j][2] += clusterget[j][2]; //dodaj
                        clusters[j][3] += clusterget[j][3];
                        clusters[j][4] += clusterget[j][4];
                    }
                }
                //smo konec?
                changed = false;
                for (int i = 0; i < clusters.length; i++) { //delimo z št. el na clusterju za povprečje
                    if (clusters[i][4] != 0) { //ne smemo delit z 0
                        clusters[i][2] = clusters[i][2] / clusters[i][4];
                        clusters[i][3] = clusters[i][3] / clusters[i][4];
                         System.out.println(clusters[i][2] + " " + clusters[i][3] + " st tock " + clusters[i][4] + " old " + clusters[i][0] + " " + clusters[i][1]);
                        if (clusters[i][0] != clusters[i][2] && clusters[i][1] != clusters[i][3]) { //pogledamo če sta še vedno enaka
                            changed = true;
                        }
                    }
                }
                System.out.println(" ");
                if (!changed) { //smo konec
                    System.out.println("konec");
                    break;
                } else { //prestavi new na old
                    for (int i = 0; i < clusters.length; i++) {
                        clusters[i][4] = 0; //ponastavi st. el. na clusterju
                        if (clusters[i][2] != 0 || clusters[i][3] != 0) {
                            clusters[i][0] = clusters[i][2]; //ponastavi
                            clusters[i][1] = clusters[i][3]; //ponastavi
                            clusters[i][2] = 0; //new
                            clusters[i][3] = 0;//new
                        }
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////
        t0 = System.currentTimeMillis() - t0;
        double CasPorazdeljeni = t0;
        System.out.println("Cas porazdeljeni: " + CasPorazdeljeni);
        MPI.Finalize();

    }
}
// javac -cp .;%MPJ_HOME%/lib/mpj.jar NoJson.java
// mpjrun.bat -np 2 NoJson
