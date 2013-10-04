#!/bin/sh
# Initialize our own variables:
cmd=""
username="huangp" # git repo username
password="" # git repo password
gpg="" # gpg passphrase
args="" # extra arguments

show_help() {
    echo "usage:"
    echo "$0 [e|X] -u <git repo username> -p <git repo password> -g <gpg passphrase>"
}

OPTIND=1 # Reset is necessary if getopts was used previously in the script.  It is a good idea to make this local in a function.
while getopts "h?u:p:g:eX" opt; do
    case "$opt" in
        h|\?)
            show_help
            exit 0
            ;;
        u)  username=$OPTARG
            ;;
        p)  password=$OPTARG
            ;;
        g)  gpg=$OPTARG
            ;;
        e)  args="$args -e"
            ;;
        X)  args="$args -X"
            ;;
    esac
done

shift $((OPTIND-1)) # Shift off the options and optional --.

cmd="mvn $args release:clean release:prepare -Dusername=$username -Dpassword=$password -Dgpg.passphrase=$gpg $@"

echo "[debug] cmd=$cmd"

# should make sure settings.xml contains gpg profile and activated by default
# <profile>
#    <id>gpg</id>
#    <properties>
#      <gpg.executable>gpg2</gpg.executable>
#      <gpg.passphrase>${gpg.passphrase}</gpg.passphrase>
#    </properties>
#  </profile>

if [ -z "$username" -o -z "$password" -o -z "$gpg" ]
then
   show_help
   exit
fi

$cmd

rc=$?
if [[ $rc == 0 ]]
then
    echo "[debug] about to perform the release"
    mvn release:perform -Dggp.passphrase=$gpg
fi


