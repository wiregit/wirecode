#!/usr/bin/env perl

my $cnt = 0;
my $inside = 0;
while (<>) {
    next if m/\/\//;
    $inside = 1 if m/\/\*/;
    $cnt += 1 if !$inside;
    $inside = 0 if m/\*\//;
}
print $cnt . "\n"; 
