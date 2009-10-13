#!/usr/bin/env perl

my $java = "java -ea -Xmx128M -cp dapper.jar";

system("make jars");

my @pids = ();

printf("\nPress ENTER to exit the test.\n");

push(
    @pids,
    forkAndExec(
            "$java dapper.ui.FlowManagerDriver"
          . " --port 12121 --archive dapper-ex.jar ex.SimpleTest"
    )
);

sleep(2);

for ( my $i = 0 ; $i < 4 ; $i++ ) {
    push( @pids,
        forkAndExec("$java dapper.client.ClientDriver --host localhost:12121")
    );
}

for ( ; getc(STDIN) != '\n' ; ) {
}

for my $pid (@pids) {
    kill( 9, $pid );
}

sub forkAndExec {

    my ($execArg) = @_;

    $pid = fork();

    if ( $pid == 0 ) {
        exec($execArg);
    }

    $pid;
}
