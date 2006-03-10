package com.limegroup.gnutella.licenses;

/** A license that can have its name set. */
interface NamedLicense extends License {
    void setLicenseName(String name);
}