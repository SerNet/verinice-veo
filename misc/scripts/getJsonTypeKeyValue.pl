#!/usr/bin/perl

my $id="";
my $name="";
my $printedId="";
my $idOnly=0;

if ( !@ARGV || $ARGV[0] eq "--help" ) {
	print <<EOF;

getJsonTypeKeyValue.pl:

Extract attribute IDs and descriptions from JSON files. Can be used to fill in new attributes in a translation map.

USAGE: getJsonTypeKeyValue.pl [--help] [--id-only] *.json

EOF
	exit;
}
if ( $ARGV[0] eq "--id-only" ) {
	$idOnly=1;
	shift @ARGV;
}

while (<>) {
    chomp;
    if (m/"(process_.*?)":/) {
        $id=$1;
    }
    if ($id && !$name && m/"title": "(.*?)",/) {
        $name=$1;
    }
	if ($id && $name && !($id eq $printedId)) {
		if ($idOnly) {
			print "$id\n";
		} else {
    		print "\"$id\": \"$name\",\n";
		}
		$printedId = $id;
		$id="";
		$name="";
	}
}
