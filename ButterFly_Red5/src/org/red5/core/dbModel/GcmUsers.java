package org.red5.core.dbModel;
// default package
// Generated Mar 29, 2014 3:32:38 PM by Hibernate Tools 4.0.0

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * GcmUsers generated by hbm2java
 */
@Entity
@Table(name = "gcm_users")
public class GcmUsers implements java.io.Serializable {

	private Integer id;
	private Set<GcmUserMails> gcmUserMailses = new HashSet<GcmUserMails>(0);
	private Set<StreamViewers> streamViewerses = new HashSet<StreamViewers>(0);
	private Set<RegIds> regIdses = new HashSet<RegIds>(0);
	private Set<Streams> streams = new HashSet<Streams>(0);
	
	public GcmUsers() {
	}

	public GcmUsers(Set<GcmUserMails> gcmUserMailses,
			Set<StreamViewers> streamViewerses, Set<RegIds> regIdses) {
		this.gcmUserMailses = gcmUserMailses;
		this.streamViewerses = streamViewerses;
		this.regIdses = regIdses;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "gcmUsers")
	public Set<GcmUserMails> getGcmUserMailses() {
		return this.gcmUserMailses;
	}

	public void setGcmUserMailses(Set<GcmUserMails> gcmUserMailses) {
		this.gcmUserMailses = gcmUserMailses;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "gcmUsers")
	public Set<StreamViewers> getStreamViewerses() {
		return this.streamViewerses;
	}

	public void setStreamViewerses(Set<StreamViewers> streamViewerses) {
		this.streamViewerses = streamViewerses;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "gcmUsers")
	public Set<RegIds> getRegIdses() {
		return this.regIdses;
	}

	public void setRegIdses(Set<RegIds> regIdses) {
		this.regIdses = regIdses;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "gcmUsers")
	public Set<Streams> getStreams() {
		return streams;
	}

	public void setStreams(Set<Streams> streams) {
		this.streams = streams;
	}
	
	

}
