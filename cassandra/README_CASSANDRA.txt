This readme is about setting up DB for persistence mechanism.

1. Download and setup Cassandra.
    To do so, follow instructions from: http://wiki.apache.org/cassandra/GettingStarted . 
    Those are fairly simple, steps.
    
2. Start Cassandra
    Execute
        cd $CASSANDRA_HOME
        ./bin/cassandra -f
    
    This will start DB with console in current terminal

3. Create DB.
    Execute in CQL3:
CREATE KEYSPACE "saturn"
         WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};
USE "saturn";




Query Analysis


