#!/contrib/bin/perl

my ($i, $n, $prefix, $suffix);

if (@ARGV < 2) {
    die "touchX filename_prefix number_of_files [suffix]\n";
}

$prefix = shift;
$n = shift;
$suffix = shift;

for ($i = 1; $i <= $n; $i++) {
    $r= int (rand 1000000);
    system('touch', "$prefix$r.$suffix");
}
