#!bash
jar -cvf core.jar `du -a | grep "\.class" |awk '{print $2;}'`
