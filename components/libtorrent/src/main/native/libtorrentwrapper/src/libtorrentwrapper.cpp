#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <memory>

#include "libtorrent/session.hpp"
#include  "boost/shared_ptr.hpp"
#include "libtorrent/alert.hpp"
#include "libtorrent/alert_types.hpp"
#include "libtorrent/peer_id.hpp"
#include <boost/filesystem/path.hpp>
#include "libtorrent/size_type.hpp"

#ifdef WINDOWS
#include <windows.h>
#endif

libtorrent::session s;
std::string savePath;
typedef libtorrent::big_number sha1_hash;

//TODO fix memory leaks

extern "C" int init(const char* path) {
	std::string newPath(path);
	savePath = newPath;
	s.set_alert_mask(0xffffffff);
	s.listen_on(std::make_pair(6881, 6889));
	return 1;
}

extern "C" int abort_torrents() {
	s.abort();
	return 1;
}

std::string* getSizeTypeString(libtorrent::size_type size) {
	std::stringstream oss;
	oss << size;
	std::string* sizeString = new std::string(oss.str().c_str());
	//TODO clean memory
	return sizeString;
}

std::string* getSha1String(sha1_hash sha1) {
	std::stringstream oss;
	oss << sha1;

	std::string* sha1String = new std::string(oss.str().c_str());
	//TODO clean memory
	return sha1String;
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

struct torrent_s {
	const char* total_done;
	const char* total_download;
	const char* total_upload;
	float download_rate;
	float upload_rate;
	int num_peers;
	int num_uploads;
	int num_seeds;
	int num_connections;
	int state;
	float progress;
	int paused;
	int finished;
	int valid;
};

void get_torrent_s(libtorrent::torrent_handle handle, torrent_s* stats) {
	libtorrent::torrent_status status = handle.status();

	float download_rate = status.download_rate;
	float upload_rate = status.upload_rate;
	//TODO cleanup memory
	const char* total_done = getSizeTypeString(status.total_done)->c_str();
	const char* total_download =
			getSizeTypeString(status.total_download)->c_str();
	const char* total_upload = getSizeTypeString(status.total_upload)->c_str();
	int num_peers = status.num_peers;
	int num_uploads = status.num_uploads;
	int num_seeds = status.num_seeds;
	int num_connections = status.num_connections;
	int state = status.state;
	float progress = status.progress;
	bool paused = status.paused;
	bool finished = handle.is_finished();
	bool valid = handle.is_valid();

	stats->total_done = total_done;
	stats->total_download = total_download;
	stats->total_upload = total_upload;
	stats->download_rate = download_rate;
	stats->upload_rate = upload_rate;
	stats->num_peers = num_peers;
	stats->num_uploads = num_uploads;
	stats->num_seeds = num_seeds;
	stats->num_connections = num_connections;
	stats->state = state;
	stats->progress = progress;
	stats->paused = paused;
	stats->finished = finished;
	stats->valid = valid;

}

libtorrent::torrent_handle findTorrentHandle(const char* sha1String) {
	sha1_hash sha1 = getSha1Hash(sha1String);
	libtorrent::torrent_handle torrent_handle = s.find_torrent(sha1);
	return torrent_handle;
}

struct info_s {
	const char* sha1;
	const char* name;
	int piece_length;
	int num_pieces;
	int num_files;
	const char* content_length;
	const char** paths;
};

extern "C" int move_torrent(const char* id, const char* path) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		std::string newPath(path);
		h.move_storage(newPath);
		return 1;
	} else {
		return 0;
	}
}

extern "C" const void* add_torrent_old(char* sha1String, char* trackerURI) {
	//TODO rename method
	std::cout << "adding torrent" << std::endl;
	std::cout << "sha1String" << sha1String << std::endl;
	std::cout << "trackerURI" << trackerURI << std::endl;
	sha1_hash sha1 = getSha1Hash(sha1String);
	std::cout << "sha1_hash" << sha1 << std::endl;
	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.info_hash = sha1;
	p.tracker_url = trackerURI;

	libtorrent::torrent_handle h = s.add_torrent(p);
//	libtorrent::torrent_info torrent_info = h.get_torrent_info();
//	const char* name = torrent_info.name().c_str();
//	int piece_length = torrent_info.piece_length();
//	int num_pieces = torrent_info.num_pieces();
//	int num_files = torrent_info.num_files();
//	std::string* content_length = getSizeTypeString(torrent_info.total_size());
//
//	libtorrent::file_storage files = torrent_info.files();
//	const char** paths = new const char*[num_files];
//	for (int i = 0; i < num_files; i++) {
//		libtorrent::file_entry file = files.at(i);
//		boost::filesystem::path path = file.path;
//		const char* p = path.string().c_str();
//		paths[i] = p;
//	}
//
//	//TODO free memory
	info_s* info = new info_s();
//	info->sha1 = sha1String;
//	info->name = name;
//	info->num_files = num_files;
//	info->num_pieces = num_pieces;
//	info->piece_length = piece_length;
//	info->content_length = content_length->c_str();
//	info->paths = paths;

	return info;

}

extern "C" const void* add_torrent(char* path) {
	std::cout << "adding torrent" << std::endl;
	std::cout << "path: " << path << std::endl;
	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.ti = new libtorrent::torrent_info(path);
	libtorrent::torrent_handle h = s.add_torrent(p);

	libtorrent::torrent_info torrent_info = h.get_torrent_info();

	const char* name = torrent_info.name().c_str();
	int piece_length = torrent_info.piece_length();
	int num_pieces = torrent_info.num_pieces();
	int num_files = torrent_info.num_files();

	//TODO cleanup memory
	std::string* content_length = getSizeTypeString(torrent_info.total_size());

	std::cout << "total_size_unknown: " << torrent_info.total_size()
			<< std::endl;
	std::cout << "total_size_long: " << content_length << std::endl;

	libtorrent::file_storage files = torrent_info.files();

	const char** paths = new const char*[num_files];
	for (int i = 0; i < num_files; i++) {
		libtorrent::file_entry file = files.at(i);
		boost::filesystem::path path = file.path;
		const char* p = path.string().c_str();
		paths[i] = p;
	}

	sha1_hash sha1 = torrent_info.info_hash();
	std::cout << "sha1: " << sha1 << std::endl;

	const char* sha1String = getSha1String(sha1)->c_str();

	std::cout << "sha1String: " << sha1String << std::endl;
	//TODO free memory
	info_s* info = new info_s();
	info->sha1 = sha1String;
	info->name = name;
	info->num_files = num_files;
	info->num_pieces = num_pieces;
	info->piece_length = piece_length;
	info->content_length = content_length->c_str();
	info->paths = paths;

	return info;
}

extern "C" int pause_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.pause();
		return 1;
	} else {
		return 0;
	}

}

extern "C" int remove_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		s.remove_torrent(h);
		return 1;
	} else {
		return 0;
	}
}

extern "C" int resume_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		h.resume();
		return 1;
	} else {
		return 0;
	}
}

extern "C" void* get_torrent_status(const char* id, void* stat) {
	struct torrent_s* stats = (struct torrent_s *) stat;

	libtorrent::torrent_handle h = findTorrentHandle(id);
	if (h.is_valid()) {
		get_torrent_s(h, stats);
	} else {
		stats->valid = false;
	}
	return stats;
}

struct alert_s {
	const char* sha1;
	const char* message;
	int category;
};

extern "C" void get_alerts(void(*alertCallback)(void*, void*)) {

	std::auto_ptr<libtorrent::alert> alerts;

	alerts = s.pop_alert();

	while (alerts.get()) {
		libtorrent::alert* alert = alerts.get();
		std::string message = alert->message();
		int alertCategory = alert->category();

		alert_s* a = new alert_s();
		a->sha1 = 0;
		a->category = alertCategory;
		a->message = message.c_str();

		torrent_s* ts = new torrent_s();

		if (libtorrent::torrent_alert * torrentAlert
				= dynamic_cast<libtorrent::torrent_alert*> (alert)) {
			std::cout << "torrent_alert" << std::endl;
			libtorrent::torrent_handle handle = torrentAlert->handle;
			if (handle.is_valid()) {
				//some bad events can have invalid handles, i mean really....
				const char* sha1 = getSha1String(handle.info_hash())->c_str();
				a->sha1 = sha1;
				get_torrent_s(handle, ts);
			}
		}
		alertCallback(a, ts);
		delete a;
		delete ts;

		alerts = s.pop_alert();
	}
}

extern "C" void print() {
	std::cout << "print called!" << std::endl;
}

