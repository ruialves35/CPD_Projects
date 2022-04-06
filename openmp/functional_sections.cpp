#include <omp.h>
#include <iostream>
#include <unistd.h>



using namespace std;

int A(int th){
   cout << "Processing A, th=" << th << endl;
   usleep(1000*1000*7);
   cout << "A Done" << endl;
   return 1;
}

int B(int th){
   cout << "Processing B, th=" << th << endl;
   usleep(1000*1000*6);
   cout << "B Done" << endl;
   return 10;
}

int C(int th){
   cout << "Processing C, th=" << th << endl;
   usleep(1000*1000*5);
   cout << "C Done" << endl;
   return 20;
}

int f1(int a, int b, int th){
   cout << "Processing f1, th=" << th << endl;
   usleep(1000*1000*4);
   cout << "f1 Done" << endl;
   return a+b;
}

int f2(int a, int b, int th){
   cout << "Processing f2, th=" << th << endl;
   usleep(1000*1000*5);
   cout << "f2 Done" << endl;
   return a+b;
}

int main (int argc, char *argv[])
{	int n = 10;
	int i;
	int y=0, v=0, w=0, x,e;

	#pragma omp parallel
	{
		#pragma omp sections
		{
            v = B(omp_get_thread_num());
            #pragma omp section
            w = C(omp_get_thread_num());
        }
        #pragma omp sections
        {
            x = f1(v,w,omp_get_thread_num());
            #pragma omp section
            y = A(omp_get_thread_num());
        }
        #pragma omp single
        cout << "e= " << f2(y,x,omp_get_thread_num()) << endl;
		
	}
	
	cout << "e= " << e << endl;

	return 0;
}
