package ru.gdgkazan.popularmovies.model.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import io.realm.RealmObject;

/**
 * @author Artur Vasilov
 */
public class Video extends RealmObject implements Parcelable {

    @SerializedName("key")
    private String mKey;

    @SerializedName("name")
    private String mName;

    public Video(){}

    protected Video(Parcel in) {
        mKey = in.readString();
        mName = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @NonNull
    public String getKey() {
        return mKey;
    }

    public void setKey(@NonNull String key) {
        mKey = key;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mKey);
        parcel.writeString(mName);
    }
}
