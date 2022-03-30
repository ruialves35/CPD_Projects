# Matrix Multiplication

- Without parallelization: 5.753 secs
- With `#pragma omp parallel for`: 1.128 secs
- With `#pragma omp parallel` and `#pragma omp for`: 2.349 secs

# Loop Scheduling

Tested the different types of allocation (static, dynamic, guided) a chunk sizes

# Pipelines

Pipeline 1 took 56 secs while pipeline 2 took 38 secs.

## Questions
1. So the threads don't access the variable at the same time (race conditions)
2. In **2** and **4**, we already have the data preloaded, so we don't need to wait for the data of the next iteration.
In **1**, we can't use `nowait` because we need that data for the first iteration of the cycle. There's also a barrier
which makes the threads wait for each other before the next iteration.
3. With dynamic, the chunks are allocated to any free thread. If we were to use static, the thread busy in **2**
would also be allocated and the program would need to wait for it, losing the advantage of using `nowait`.
If we removed it completely, it'd act as a static scheduling
