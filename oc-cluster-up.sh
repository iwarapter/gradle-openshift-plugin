#!/usr/bin/env bash

oc cluster up
oc login -u system:admin
oc adm policy add-role-to-user view developer -n default