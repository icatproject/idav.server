package org.icatproject.iDav;

public class IcatEntityNames {

    private String facilityName;
    private String investigationName;
    private String datasetName;
    private String datafileName;
    private String cycleName;
    private String instrumentName;

    @Override
    public String toString() {
        return "IcatEntityNames [facilityName=" + facilityName
                + ", investigationName=" + investigationName
                + ", datasetName=" + datasetName
                + ", datafileName=" + datafileName + "]";
    }

    public String getCycleName() {
        return cycleName;
    }

    public void setCycleName(String cycleName) {
        this.cycleName = cycleName;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getInvestigationName() {
        return investigationName;
    }

    public void setInvestigationName(String investigationName) {
        this.investigationName = investigationName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatafileName() {
        return datafileName;
    }

    public void setDatafileName(String datafileName) {
        this.datafileName = datafileName;
    }

}
