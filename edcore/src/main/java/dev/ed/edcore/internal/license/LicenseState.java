package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.license.LicenseStatus;

import java.util.concurrent.atomic.AtomicReference;

/** Глобальное состояние лицензии (потокобезопасно). */
public final class LicenseState {

    private final AtomicReference<LicenseStatus> status = new AtomicReference<>(LicenseStatus.UNREGISTERED);
    private volatile String owner = "";
    private volatile String remoteStatus = "";

    public LicenseStatus getStatus() {
        return status.get();
    }

    public void setStatus(LicenseStatus newStatus) {
        status.set(newStatus);
    }

    public boolean isValid() {
        return status.get() == LicenseStatus.VALID;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner == null ? "" : owner;
    }

    public String getRemoteStatus() {
        return remoteStatus;
    }

    public void setRemoteStatus(String remoteStatus) {
        this.remoteStatus = remoteStatus == null ? "" : remoteStatus;
    }
}
