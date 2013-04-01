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
    Execute:
        cd $CASSANDRA_HOME
        ./bin/cqlsh -f  $SMSC_DIR/cassandra/cassadra.cql