package com.bottleworks.dailymoney.data;

import java.util.Date;

/**
 * 
 * @author dennis
 * 
 */
public class Detail {

    private int id;
    private String from;
    private String to;
    private Date date;
    private Double money;
    private String note;
    
    private boolean archived;

    Detail(){}

    public Detail(String fromAccount,String toAccount,Date date, Double money,
            String note) {
        this.from = fromAccount;
        this.to = toAccount;
        this.date = date;
        this.money = money;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Detail other = (Detail) obj;
        if (id != other.id)
            return false;
        return true;
    }


}