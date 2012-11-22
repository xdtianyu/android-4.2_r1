#!/bin/bash
GIT_BRANCH=`git branch | grep "*" | cut -b 3-`
if [ x$GIT_BRANCH != "x(no branch)" ]
then
	git pull origin $GIT_BRANCH
else
	echo "No branch set: Have you checked out a branch?"
fi
