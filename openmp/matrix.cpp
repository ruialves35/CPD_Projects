#include <stdio.h>
#include <omp.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <algorithm>

using namespace std;

void OnMultLine(int m_ar, int m_br)
{
    clock_t Time1, Time2;

    char st[100];

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = (double)1.0;

    for (int i = 0; i < m_br; i++)
        for (int j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    for (int i = 0; i < m_ar; i++)
        for (int j = 0; j < m_ar; j++)
            phc[i * m_ar + j] = (double)0;

    Time1 = clock();

    #pragma omp parallel
    for (int i = 0; i < m_ar; i++)
    {
        for (int k = 0; k < m_ar; k++)
        {
            #pragma omp for
            for (int j = 0; j < m_br; j++)
            {
                phc[i * m_ar + j] += pha[i * m_ar + k] * phb[k * m_br + j];
            }
        }
    }

    Time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC / omp_get_num_procs());
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
    cout << "Result matrix: " << endl;
    for (int i = 0; i < 1; i++)
    {
        for (int j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}

int main() {
    OnMultLine(1000, 1000);
    return 0;
}
