import mpi.MPI;

import java.text.DecimalFormat;
import java.util.Random;

public class NoJson {

    public static void main(String[] args) {
        //INIT
        double t0 = System.currentTimeMillis();
        MPI.Init(args);
        Random rand = new Random();
        int cluster = 5;
        int tras = 12932;
        double[][] clusters = new double[cluster][5]; //old, old, new, new, št točk

        float[][] data = new float[tras][2]; //random točke
        boolean changed;
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        double[][] clusterget = new double[cluster][5];
        int chunki = tras / size;
        int maxi;
        int[] trash = new int[tras]; //pripada
        DecimalFormat df=new DecimalFormat("#.##");
        //////////////////////////////////////////////////////////////
        for (int i = 0; i < tras; i++) {
            data[i][0] = (float) (rand.nextFloat() * (55.1 - 46) + 46); //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
            data[i][1] = (float) (rand.nextFloat() * (17.5 - 5.5) + 5.5); //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
        }

        while (true) {
            // System.out.println("kje smo " + me);
            //DELAJ
            if (me == 0) {
                if (clusters[0][0] == 0) {//generiraj podatke
                    for (int i = 0; i < clusters.length; i++) {
                        clusters[i][0] = rand.nextDouble() * (55.1 - 46) + 46; //old NASTAVI GLEDE NA GEO. VIŠINO, ČE JE PREVEČ BO SLABO NATANČEN
                        clusters[i][1] = rand.nextDouble() * (17.5 - 5.5) + 5.5; //old  NASTAVI GLEDE NA GEO. ŠIRINO, ČE JE PREVEČ BO SLABO NATANČEN
                        clusters[i][2] = 0; //new
                        clusters[i][3] = 0;
                        clusters[i][4] = 0;
                    }

                    //

                }
                for (int i = 1; i < size; i++) {
                    MPI.COMM_WORLD.Send(clusters, 0, clusters.length, MPI.OBJECT, i, MPI.ANY_TAG);
                }

                // Izracunaj
                for (int j = 0; j < chunki; j++) {

                    float mini = (float) Math.hypot(Math.abs(clusters[0][0] - data[j][0]), Math.abs(clusters[0][1] - data[j][1]));
                    //(clusters[0][2], clusters[0][3], data.get("la"), data.get("lo")); //prvi cluster

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

                //prejmi
                for (int i = 1; i < size; i++) {
                    MPI.COMM_WORLD.Recv(clusterget, 0, clusterget.length, MPI.OBJECT, i, MPI.ANY_TAG);
                    for (int j = 0; j < clusters.length; j++) {
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
                         System.out.println(df.format(clusters[i][2])+" "+ df.format(clusters[i][3])+" st tock "+clusters[i][4]+" old "+df.format(clusters[i][0])+" "+ df.format(clusters[i][1]));
                        if (!df.format(clusters[i][0]).equals(df.format(clusters[i][2])) && !df.format(clusters[i][1]).equals(df.format(clusters[i][3]))) { //pogledamo če sta še vedno enaka
                            changed = true;
                        }

                    }
                }
                System.out.println(" cikel");
                if (!changed) { //smo konec
                    System.out.println("kje smo???");

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
            /////////////////////////////////////////////////////////////////////////////////////////////
            else {
//                for (int i = 0; i < clusters.length; i++) {
//                    clusters[i][0] = clusters[i][2]; //old
//                    clusters[i][1] = clusters[i][3]; //old
//                    clusters[i][2] = 0; //new
//                    clusters[i][3] = 0;//new
//                    clusters[i][4] = 0;
//                }
                MPI.COMM_WORLD.Recv(clusters, 0, clusters.length, MPI.OBJECT, 0, MPI.ANY_TAG);
                if (size != me + 1)
                    maxi = me * chunki + chunki;
                else
                    maxi = data.length;

                for (int j = me * chunki; j < maxi; j++) {
                    float mini = (float) Math.hypot(Math.abs((float) clusters[0][0] - data[j][0]), Math.abs((float) clusters[0][1] - data[j][1]));

                    for (int k = 0; k < clusters.length; k++) {
                        if ((float) Math.hypot(Math.abs((float) clusters[k][0] - data[j][0]), Math.abs((float) clusters[k][1] - data[j][1])) <= mini) { //če je drug cluster bližji zamenjaj
                            mini = (float) Math.hypot(Math.abs((float) clusters[k][0] - data[j][0]), Math.abs((float) clusters[k][1] - data[j][1]));
                            trash[j] = k; //katermu clusterju pripada
                        }
                    }
                    clusters[trash[j]][4]++;//št el. na clusterju
                    clusters[trash[j]][2] += data[j][0]; //seštevamo sproti
                    clusters[trash[j]][3] += data[j][1]; //seštevamo sproti
                }
//                for (double[] doubles : clusters) {
//                    System.out.println(doubles[2] + " " + doubles[3] + " st tock " + doubles[4] + " old " + doubles[0] + " " + doubles[1]);
//                }
                // poslji do main
                MPI.COMM_WORLD.Send(clusters, 0, clusters.length, MPI.OBJECT, 0, MPI.ANY_TAG);
                //MPI.COMM_WORLD.wait();
            }

        }
        t0 = System.currentTimeMillis() - t0;
        double CasPorazdeljeni = t0;
        System.out.println("Cas porazdeljeni: " + CasPorazdeljeni);
        MPI.Finalize();


    }
}
// javac -cp .;%MPJ_HOME%/lib/mpj.jar NoJson.java
// mpjrun.bat -np 2 NoJson
