#!/usr/bin/env perl
#
# Checks that in using MessageFormat.(pattern,args) the variable place
# holders start at 0 and are contiguous
#
# This is pretty elemntary and need not go in a final release, 
# but I just want to check myself here
#

# todo: what is the special variable for input file???
foreach my $infile (@ARGV) {
	open(infile,"<$infile") || die "cannot open $infile: $!";
	while (<infile>) {
		my $str = "";
		while (m/"([^"]*)"/g) {
			$str .= $1;
		}
		if ($str) {
			my @lst = ();
			while ($str =~ m/\{(\d+)\}/g) {
				push @lst, ($1);
			}
			if ($#lst >= 0) {
				@lst = sort @lst;
				my $bad = 0;
				for (my $i=0; $i <= $#lst; $i++) {
					if ($lst[$i] != $i) {
						$bad = 1;
						next;
					}
				}
				if ($bad) {print stderr $infile . ":" . $. . " " . $str . "\n"} 
			}
		}
	}
	close(infile);
}
