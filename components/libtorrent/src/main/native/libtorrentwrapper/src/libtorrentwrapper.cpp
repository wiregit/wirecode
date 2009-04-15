#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <memory>

#include "libtorrent/session.hpp"
#include  "boost/shared_ptr.hpp"
#include "libtorrent/alert.hpp"
#include "libtorrent/alert_types.hpp"
#include <dlfcn.h>
#include "libtorrent/peer_id.hpp"
#include <boost/filesystem/path.hpp>

libtorrent::session s;
std::string savePath;
typedef libtorrent::big_number sha1_hash;


//TODO fix memory leaks

extern "C" int init(const char* path) {
	std::string newPath(path);
	savePath = newPath;
	s.set_alert_mask(0xffffffff);
	s.listen_on(std::make_pair(6881, 6889));

	return 0;
}

const char* getSha1String(sha1_hash sha1) {
	std::stringstream oss;
	oss << sha1;

	std::string* sha1String = new std::string(oss.str().c_str());
	//TODO clean memory
	return sha1String->c_str();
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

struct torrent_s {
	long total_done;
	float download_rate;
	int num_peers;
	int state;
	float progress;
	int paused;
};

void get_torrent_s(libtorrent::torrent_handle handle, torrent_s* stats) {
	libtorrent::torrent_status status = handle.status();

	float download_rate = status.download_rate;
	long total_done = status.total_done;
	int num_peers = status.num_peers;
	int state = status.state;
	float progress = status.progress;
	bool paused = status.paused;

	stats->total_done = total_done;
	stats->download_rate = download_rate;
	stats->num_peers = num_peers;
	stats->state = state;
	stats->progress = progress;
	stats->paused = paused;

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
	long content_length;
	const char** paths;
};

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
	long content_length = torrent_info.total_size();

	libtorrent::file_storage files = torrent_info.files();

	const char** paths = new const char*[num_files];
	for(int i = 0; i < num_files; i++) {
		libtorrent::file_entry file = files.at(i);
		boost::filesystem::path path = file.path;
		const char* p = path.string().c_str();
		paths[i] = p;
	}

	sha1_hash sha1 = torrent_info.info_hash();
	std::cout << "sha1: " << sha1 << std::endl;

	const char* sha1String = getSha1String(sha1);

	std::cout << "sha1String: " << sha1String << std::endl;
	//TODO free memory
	info_s* info = new info_s();
	info->sha1 = sha1String;
	info->name = name;
	info->num_files = num_files;
	info->num_pieces = num_pieces;
	info->piece_length = piece_length;
	info->content_length = content_length;
	info->paths = paths;

	return info;
}

extern "C" int pause_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	h.pause();
	return 0;
}

extern "C" bool is_torrent_paused(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	return h.is_paused();
}

extern "C" bool is_torrent_seed(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	return h.is_seed();
}

extern "C" const char* get_torrent_name(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	return h.name().c_str();
}

extern "C" bool is_torrent_finished(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	return h.is_finished();
}

extern "C" bool is_torrent_valid(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	std::cout << "is_torrent_valid: " << h.is_valid() << std::endl;
	return h.is_valid();
}

extern "C" int resume_torrent(const char* id) {
	libtorrent::torrent_handle h = findTorrentHandle(id);
	h.resume();
	return 0;
}

extern "C" void* get_torrent_status(const char* id, void* stat) {
	struct torrent_s* stats = (struct torrent_s *) stat;

	libtorrent::torrent_handle h = findTorrentHandle(id);

	get_torrent_s(h, stats);
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
				const char* sha1 = getSha1String(handle.info_hash());
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

void Alert(void* alert, void* stats) {
	alert_s* a = (alert_s*) alert;
	std::cout << a->message << std::endl;
}

int main(int argc, char* argv[]) {
	try {
		init("/home/pvertenten/Desktop");
		info_s* info =(info_s*)
						add_torrent(
								"/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent");

		const char* id = info->sha1;
		int count = 1;
		bool paused = false;
		while (true) {
			std::cout << "paused: " << is_torrent_paused(id) << std::endl;
			std::cout << "finished: " << is_torrent_finished(id) << std::endl;
			std::cout << "valid: " << is_torrent_valid(id) << std::endl;
			if (count % 30 == 0) {
				if (paused) {
					resume_torrent(id);
					paused = false;
				} else {
					pause_torrent(id);
					paused = true;
				}
			}
			get_alerts(Alert);
			std::cout << "status: " << std::endl;
			//			struct torrent_s s = get_torrent_status("id");
			//			std::cout << "total_payload_download: " << s.total_payload_download
			//					<< std::endl;
			count++;
			sleep(1);
		}
	} catch (std::exception& e) {
		std::cout << e.what() << "\n";
	}
	return 0;
}

