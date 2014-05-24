package org.red5.core.dbModel;

// Generated May 24, 2014 11:23:18 AM by Hibernate Tools 4.0.0

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
	private Set<Streams> streamses = new HashSet<Streams>(0);
	private Set<StreamViewers> streamViewerses = new HashSet<StreamViewers>(0);
	private Set<RegIds> regIdses = new HashSet<RegIds>(0);

	public GcmUsers() {
	}

	public GcmUsers(Set<GcmUserMails> gcmUserMailses, Set<Streams> streamses,
			Set<StreamViewers> streamViewerses, Set<RegIds> regIdses) {
		this.gcmUserMailses = gcmUserMailses;
		this.streamses = streamses;
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
	public Set<Streams> getStreamses() {
		return this.streamses;
	}

	public void setStreamses(Set<Streams> streamses) {
		this.streamses = streamses;
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

}
