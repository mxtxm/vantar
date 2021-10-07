#!/usr/bin/env bash
echo "starting..."
echo "creating newser user and db as root"
BASEDIR=$(dirname $0)
echo $BASEDIR
psql -f $BASEDIR/db.sql
echo "seems fine.."
echo "creating tables as newsـparser user.."
psql -U newser -h localhost news_parser -f $BASEDIR/relations.sql
echo "finished!"
