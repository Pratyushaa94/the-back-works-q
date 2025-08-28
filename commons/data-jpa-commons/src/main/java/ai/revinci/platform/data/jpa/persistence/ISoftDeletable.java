package ai.revinci.platform.data.jpa.persistence;


public interface ISoftDeletable {

    boolean isDeleted();

    void setDeleted(boolean deleted);

   String getDeletedBy();

    void setDeletedBy(String deletedBy);

    Long getDeletedDate();

    void setDeletedDate(Long deletedDate);
}