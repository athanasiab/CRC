//GitHub: athanasiab

import java.util.Random;
import java.util.Scanner;

public class CRC {

    public static void main(String[] args){
        final long messages = 10000000000L; //by changing the constant we change the number of messages that will be transmitted
        final int ber = 3; //by changing the constant we change the B.E.R. that causes the noise

        Scanner reader = new Scanner(System.in);
        boolean isValid = true; //holds whether the number given by the user is binary
        int numP; //holds the inserted P by the user
        int[] temp; //holds value of p as array of ints
        boolean[] p; //number p stored as array of booleans

        int n; //number of bits of T (the message that will be transmitted)
        int k; //number of bits in D (the initial message)
        int pBits; //number of bits in P (the number we'll divide by)

        long allE = 0L; //counter all the messages transmitted with errors
        long errors = 0L; //counter for errors found

        double percE; //percentage of messages with errors
        double percN; //percentage of messages with errors that haven't been detected
        double percD; //percentage of messages with errors that have been detected

        boolean[] t; //the final message (holds every form of the message)

        //asks for number P from the user
        System.out.print("Give number P: ");
        numP = reader.nextInt();

        reader.reset();
        System.out.print("Give number of bits (k): ");
        k = reader.nextInt();

        //sets the p
        pBits = String.valueOf(numP).length();
        p = new boolean[pBits];
        temp = new int[pBits];

        //sets p and checks for the validity of the number (if it is binary and whether the first and last digit is 1)
        for(int i = 0; i < pBits; i++) {
            temp[i] = numP / (int)Math.pow(10, pBits-i-1);
            numP = numP % (int)Math.pow(10, pBits-i-1);
            if((temp[i] != 0 && temp[i] != 1) || (i == 0 || i == pBits-1) && temp[i] != 1){
                isValid = false;
                System.out.println("Not valid P");
                break;
            }
        }
        if(isValid) { //only does the following actions if P is a valid number
            System.arraycopy(TurnBool(temp, pBits), 0, p, 0, pBits); //set the boolean array for number p

            n = k + pBits - 1; //size of message that will be transmitted
            t = new boolean[n+1]; //last cell is used to identify whether the message has been changed or not (used in classes)

            for (long i = 0L; i < messages; i++){
                //creates message
                System.arraycopy(NumGenerator(k), 0, t, 0, k); //creates and stores the initial message for further processing

                //fills ending with zeros
                for(int j=k; j < n+1; j++){
                    t[j] = false;
                }

                //finds the remainder of the division by p
                System.arraycopy(Remainder(t, p, n, pBits), 1, t, k, pBits); //adds the FCS (the remainder of the division by p)

                //noise is is being applied
                System.arraycopy(Noise(t, n, ber), 0, t, 0, n+1); //the message after noise applied + 1 bit for indicating change made
                if(t[n]){ //if the message was influenced by the noise
                    allE++; //increases counter of error messages
                }

                //checks if error has been detected
                //the remainder should be filled with zeros in order for the message to be recognised as solid
                if(Remainder(t, p, n, pBits)[pBits]){ //has found digit "1", thus it has detected error (and last cell is filled with the value true)
                    errors++; //increases errors' found counter
                }

            }

            //calculates the percentages
            percE = Percentage(allE, messages); //all errors to all messages
            percN = Percentage(allE - errors, messages); //errors not found to all messages
            percD = Percentage(errors, messages); //errors found to all messages
            Show(messages, ber, percE, percN, percD); //shows the statistics/results on the screen
        }

    }

    //turns integer array into array of boolean (handles message as array of boolean variables to take up less space in memory)
    static private boolean[] TurnBool(int[] a, int k){
        boolean[] b = new boolean[k];
        for(int i=0; i<k; i++){
            b[i] = a[i] == 1; //1 is represented by true and 0 by false
        }
        return b;
    }

    //implements the binary division and returns the remainder of it
    static private boolean[] Remainder(boolean[] t, boolean[] p, int sizeT, int sizeP){ //t (or 2^(n-k)D) gets divided by p, sizeT=size of t, sizeP=size of t
        boolean[] a = new boolean[sizeP+1]; //the part of "t" that will be divided for every step of the division
        boolean[] r = new boolean[sizeP]; //holds the remainder
        int counter = 0; //counts in which index of t we are going to extract the next value
        boolean s = false; //holds whether it has remainder or not

        for(int i = 0; i < sizeP; i++){ //initializes a
            a[i] = t[i];
            counter++;
        }

        while(counter < sizeT || a[0]){ //while there are values left to t
            //if the first digit of a is false(0) it adds values from t at its end
            while(!a[0] && counter < sizeT){
                //moves all values one index to the left
                System.arraycopy(a, 1, a, 0, sizeP - 1);
                a[sizeP-1] = t[counter]; //brings down the next value that can be used by t
                counter++; //moves to the next index that will be used
            }
            //performs binary division
            if(a[0]) { //can perform division only if the first bit of a is 1
                for (int i = 0; i < sizeP; i++) {
                    r[i] = a[i] ^ p[i];
                    if (counter == sizeT && r[i] && i != 0) {
                        s = true; //there is a 1 in the remainder (non zero remainder)
                    }
                }
                System.arraycopy(r, 0, a, 0, sizeP); //sets new a equal to current r
            }else if(counter == sizeT){ //reached the end of t
                for (int i = 0; i < sizeP; i++) {
                    if (a[i]) { //checks for bits of value 1 (true)
                        s = true; //there is a 1 in the remainder (non zero remainder)
                        break;
                    }
                }
            }
        }
        a[sizeP] = s;
        return a; //"a" holds the last remainder
    }

    //produces random booleans that are the original messages
    static private boolean[] NumGenerator(int k){ //k=number of bits of the original message
        Random rand = new Random();
        boolean[] x = new boolean[k];
        for(int i=0; i < k; i++){
            x[i] = (rand.nextBoolean());
        }
        return x;
    }

    //produces noise for the message that will be transmitted
    //returns the message after noise has been applied and the last cell of the array indicates whether changes were made or not
    static private boolean[] Noise(boolean[] t, int n, int ber){ //T is the final message that will be transmitted and n = number of bits in message, ber=Bit Error Rate
        Random rand = new Random();
        boolean[] a = new boolean[n+1];
        boolean change = false; //holds whether the message has changed or not
        for(int i=0; i < n; i++) {
            if (1 == rand.nextInt((int) Math.pow(10, ber))) { //chances 1/10^n that a bit will be changed (the bit changes when value of rand ==1)
                change = true; //at least one bit has changed in the message
                a[i] = !t[i]; //changes the bit
            }else{
                a[i] = t[i];
            }
        }
        a[n] = change; //last bit indicates if there were changes made
        return a;
    }

    //class that returns the percentage of the given values
    static private double Percentage(long part, long whole){
        return (part/(double)whole)*100L;
    }

    //shows to the screen, information about the data that have been transmitted
    static private void Show(long messages, int ber, double percE, double percN, double percD){
        System.out.println("Messages sent: " + messages);
        System.out.println("B.E.R.: " + ber);
        System.out.println("Messages with errors " + percE + "%");
        System.out.println("Messages with errors that have not been detected: " + percN + "%");
        System.out.println("Messages with errors that have been detected: " + percD + "%");
    }
}
