package com.cpd_proj1;

import java.util.Scanner;

import static java.lang.Math.min;

public class Main {

    static void OnMult(int m_ar, int m_br)
    {
        double temp;
        int i, j, k;

        double[][] pha = new double[m_ar][m_ar];
        double[][] phb = new double[m_ar][m_ar];
        double[][] phc = new double[m_ar][m_ar];

        for (i = 0; i < m_ar; i++)
            for (j = 0; j < m_ar; j++)
                pha[i][j] = 1.0;

        for (i = 0; i < m_br; i++)
            for (j = 0; j < m_br; j++)
                phb[i][j] = (double) i+1;

        // START COUNTING
        long start = System.currentTimeMillis();

        for (i = 0; i < m_ar; i++)
        {
            for (j = 0; j < m_br; j++)
            {
                temp = 0;
                for (k = 0; k < m_ar; k++)
                {
                    temp += pha[i][k] * phb[k][j];
                }
                phc[i][j] = temp;
            }
        }

        long end = System.currentTimeMillis();
        double duration = (double)(end-start)/1000;
        System.out.println("Elapsed time " + duration + "s");

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for (i = 0; i < 1; i++)
        {
            for (j = 0; j < min(10, m_br); j++)
                System.out.print(phc[i][j] + " ");
        }
        System.out.println();
    }

    // add code here for line x line matriz multiplication
    static void OnMultLine(int m_ar, int m_br)
    {
        int i, j, k;

        double[][] pha = new double[m_ar][m_ar];
        double[][] phb = new double[m_ar][m_ar];
        double[][] phc = new double[m_ar][m_ar];

        for (i = 0; i < m_ar; i++)
            for (j = 0; j < m_ar; j++)
                pha[i][j] = 1.0;

        for (i = 0; i < m_br; i++)
            for (j = 0; j < m_br; j++)
                phb[i][j] = (double) i+1;

        // START COUNTING
        long start = System.currentTimeMillis();

        for (i = 0; i < m_ar; i++)
        {
            for (k = 0; k < m_ar; k++)
            {
                for (j = 0; j < m_br; j++)
                {
                    phc[i][j] += pha[i][k] * phb[k][j];
                }
            }
        }

        long end = System.currentTimeMillis();
        double duration = (double)(end-start)/1000;
        System.out.println("Elapsed time " + duration + "s");

        // display 10 elements of the result matrix tto verify correctness
        System.out.println("Result matrix: ");
        for (i = 0; i < 1; i++)
        {
            for (j = 0; j < min(10, m_br); j++)
                System.out.print(phc[i][j] + " ");
        }
        System.out.println();
    }

    // add code here for block x block matriz multiplication
    void OnMultBlock(int m_ar, int m_br, int bkSize)
    {
    }

    public static void main(String[] args) {
        char c;
        int lin, col, blockSize;
        int op;

        Scanner scanner = new Scanner(System.in);

        op = 1;
        do
        {
            System.out.println("Running Program in Java");
            System.out.println("1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Block Multiplication");
            System.out.println("Selection?:");

            op = scanner.nextInt();
            if (op == 0)
                break;
            System.out.println("Dimensions: lins=cols ? ");
            lin = scanner.nextInt();
            col = lin;

            switch (op)
            {
                case 1:
                    Main.OnMult(lin, col);
                    break;
                case 2:
                    Main.OnMultLine(lin, col);
                    break;
                case 3:
                    System.out.println("Block Size? ");
                    blockSize = scanner.nextInt();
                    // OnMultBlock(lin, col, blockSize);
                    break;
            }
        } while (true);
    }
}
