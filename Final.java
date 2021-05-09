import mpi.MPI;

import java.text.DecimalFormat;
import java.util.Random;

public class NoJson {

    public static void main(String[] args) {
        //INIT
        double t0 = System.currentTimeMillis();
        MPI.Init(args);
        Random rand = new Random();
        int cluster =5;
        int tras = 12932;
        float[][] clusters = new float[cluster][5]; //old, old, new, new, št točk
        float[][] data = new float[tras][2]; //random točke
        boolean changed;
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        float[][] clusterget = new float[cluster * size][5];
        int chunki = tras / size;
        int maxi;
        int[] trash = new int[tras]; //pripada
        //////////////////////////////////////////////////////////////
        for (int i = 0; i < tras; i++) {
            data[i][0] = (float) (rand.nextFloat() * (55.1 - 46) + 46); //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
            data[i][1] = (float) (rand.nextFloat() * (17.5 - 5.5) + 5.5); //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
        }

        while (true) {
            //DELAJ
            if (me == 0) {
                if (clusters[0][0] == 0) {//generiraj podatke
                    for (int i = 0; i < clusters.length; i++) {
                        clusters[i][0] = (float) (rand.nextFloat() * (55.1 - 46) + 46); //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
                        clusters[i][1] = (float) (rand.nextFloat() * (17.5 - 5.5) + 5.5); //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
                        clusters[i][2] = 0; //new
                        clusters[i][3] = 0;
                        clusters[i][4] = 0;
                    }

                }
            }
            MPI.COMM_WORLD.Scatter(clusters, 0, clusters.length/size, MPI.OBJECT, clusters, 0, clusters.length/size, MPI.OBJECT, 0);
            MPI.COMM_WORLD.Barrier();

            if (size != me + 1)
                maxi = me * chunki + chunki;
            else
                maxi = data.length;
            // Izracunaj

            for (int j = 0; j < maxi; j++) {
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

// for (float[] doubles : clusters) {
//                    System.out.println(doubles[2] + " " + doubles[3] + " st tock " + doubles[4] + " old " + doubles[0] + " " + doubles[1]);
//                }

            //prejmi
            MPI.COMM_WORLD.Gather(clusters, 0, clusters.length/size, MPI.OBJECT, clusterget, 0, clusters.length/size, MPI.OBJECT, 0);

            if (me == 0) {System.out.println(clusterget[0][4]+" dela");
                for (int j = 0; j < clusters.length; j++) {
                    for (int i = 0; i < size; i++) {
                        clusters[j][2] += clusterget[j + i * cluster][2]; //dodaj
                        clusters[j][3] += clusterget[j + i * cluster][3];
                        clusters[j][4] += clusterget[j + i * cluster][4];
                    }
                }
                //smo konec?
                changed = false;
                for (int i = 0; i < clusters.length; i++) { //delimo z št. el na clusterju za povprečje
                    if (clusters[i][4] != 0) { //ne smemo delit z 0
                        clusters[i][2] = clusters[i][2] / clusters[i][4];
                        clusters[i][3] = clusters[i][3] / clusters[i][4];
                        System.out.println(clusters[i][2] + " " + clusters[i][3] + " st tock " + clusters[i][4] + " old " + clusters[i][0] + " " + clusters[i][1]);
                        if (clusters[i][0]!=clusters[i][2] && clusters[i][1]!=clusters[i][3]) { //pogledamo če sta še vedno enaka
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
