import java.util.UUID;

public class Request {
	
	private String id;
	private String filename;
	private String scols;
	private String srows;
	private String wcols;
	private String wrows;
	private String coff;
	private String roff;
	private int rank;

    public Request(String filename, String scols, String srows, String wcols, String wrows, String coff, String roff) {
    	this.setId(UUID.randomUUID().toString());
        this.setFilename(filename);
		this.setScols(scols);
		this.setSrows(srows);
		this.setWcols(wcols);
		this.setWrows(wrows);
		this.setCoff(coff);
		this.setRoff(roff);
    }


    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }


	public String getRoff() {
		return roff;
	}


	public void setRoff(String roff) {
		this.roff = roff;
	}


	public String getSrows() {
		return srows;
	}


	public void setSrows(String srows) {
		this.srows = srows;
	}


	public String getScols() {
		return scols;
	}


	public void setScols(String scols) {
		this.scols = scols;
	}


	public String getFilename() {
		return filename;
	}


	public void setFilename(String filename) {
		this.filename = filename;
	}


	public String getWcols() {
		return wcols;
	}


	public void setWcols(String wcols) {
		this.wcols = wcols;
	}


	public String getWrows() {
		return wrows;
	}


	public void setWrows(String wrows) {
		this.wrows = wrows;
	}


	public String getCoff() {
		return coff;
	}


	public void setCoff(String coff) {
		this.coff = coff;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}

}
