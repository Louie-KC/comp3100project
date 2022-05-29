# MQ COMP3100 Session 1 2022 project
46399089 - Louie Coghill

A job scheduling client for the ds-sim found at https://github.com/distsys-MQ/ds-sim containing two scheduling algorithms. Communication occurs over a socket following the protocol defined in the user guide found on the ds-sim repo.

A copy of the ds-sim executables (29/05/2022) and sample configs are found in the `ds-sim-copy` directory.

A copy of stage specifications and reports are found in the `docs` directory.

## How to launch/run
`java Main <Options>`

Options:
* -ip (ip address)
* -p (port)

Default settings
* Default IP: 127.0.0.1
* Default Port: 50000

# Stage two
Schedules jobs on the ds-sim following a First Fit algorithm, however additions such as time aware scheduling (if a job must enter a waiting queue) and job migrations are added. Considers all resources of a server (core count, memory and disk space).

# Stage one
Schedule jobs on the ds-sim following a Largest Round Robin algoritm, where the first occurence of the largest server type is exclusively scheduled to. A server type is defined to be largest based only on its core count.