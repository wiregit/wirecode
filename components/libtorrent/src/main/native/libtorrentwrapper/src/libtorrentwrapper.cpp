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

libtorrent::session s;
std::string savePath;
typedef libtorrent::big_number sha1_hash;

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
	return oss.str().c_str();
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

libtorrent::torrent_handle findTorrentHandle(const char* sha1String) {
	sha1_hash sha1 = getSha1Hash(sha1String);
	libtorrent::torrent_handle torrent_handle = s.find_torrent(sha1);
	return torrent_handle;
}

extern "C" const char* add_torrent(const char* id, char* path) {
	std::cout << "adding torrent" << std::endl;
	std::cout << "id: " << id << std::endl;
	std::cout << "path: " << path << std::endl;
	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.ti = new libtorrent::torrent_info(path);
	libtorrent::torrent_handle h = s.add_torrent(p);

	libtorrent::torrent_info torrent_info = h.get_torrent_info();

	sha1_hash sha1 = torrent_info.info_hash();
	std::cout << "sha1: " << sha1 << std::endl;

	const char* sha1String = getSha1String(sha1);

	std::cout << "[" << sha1String << "]" << std::endl;
	return sha1String;
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

struct torrent_s {
	long total_done;
	float download_rate;
	int num_peers;
	int state;
	float progress;
	int paused;
};

extern "C" void* get_torrent_status(const char* id, void* stat) {
	struct torrent_s* stats = (struct torrent_s *) stat;

	libtorrent::torrent_handle h = findTorrentHandle(id);
	libtorrent::torrent_status status = h.status();

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

	return stats;
}

extern "C" void get_alerts(void(*alertCallback)(const char*),
		void(*torrentFinishedCallback)(const char*, const char*),
		void(*torrentPausedCallback)(const char*, const char*),
		void(*torrentResumedCallback)(const char*, const char*)) {

	std::auto_ptr<libtorrent::alert> alerts;

	alerts = s.pop_alert();

	while (alerts.get()) {
		libtorrent::alert* alert = alerts.get();
		std::string message = alert->message();
		int alertCategory = alert->category();
		if (libtorrent::torrent_finished_alert * a
				= dynamic_cast<libtorrent::torrent_finished_alert*> (alert)) {
			std::cout << "torrent_finished_alert" << std::endl;
			libtorrent::torrent_handle handle = a->handle;
			const char* sha1 = getSha1String(handle.info_hash());
			torrentFinishedCallback(sha1, message.c_str());
		} else if (libtorrent::external_ip_alert * a
				= dynamic_cast<libtorrent::external_ip_alert*> (alert)) {
			std::cout << "external_ip_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::listen_failed_alert * a
				= dynamic_cast<libtorrent::listen_failed_alert*> (alert)) {
			std::cout << "listen_failed_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::portmap_error_alert * a
				= dynamic_cast<libtorrent::portmap_error_alert*> (alert)) {
			std::cout << "portmap_error_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::portmap_alert * a
				= dynamic_cast<libtorrent::portmap_alert*> (alert)) {
			std::cout << "portmap_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::file_error_alert * a
				= dynamic_cast<libtorrent::file_error_alert*> (alert)) {
			std::cout << "file_error_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::tracker_announce_alert * a
				= dynamic_cast<libtorrent::tracker_announce_alert*> (alert)) {
			std::cout << "tracker_announce_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::tracker_error_alert * a
				= dynamic_cast<libtorrent::tracker_error_alert*> (alert)) {
			std::cout << "tracker_error_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::tracker_reply_alert * a
				= dynamic_cast<libtorrent::tracker_reply_alert*> (alert)) {
			std::cout << "tracker_reply_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::dht_reply_alert * a
				= dynamic_cast<libtorrent::dht_reply_alert*> (alert)) {
			std::cout << "dht_reply_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::tracker_warning_alert * a
				= dynamic_cast<libtorrent::tracker_warning_alert*> (alert)) {
			std::cout << "tracker_warning_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::scrape_reply_alert * a
				= dynamic_cast<libtorrent::scrape_reply_alert*> (alert)) {
			std::cout << "scrape_reply_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::scrape_failed_alert * a
				= dynamic_cast<libtorrent::scrape_failed_alert*> (alert)) {
			std::cout << "scrape_failed_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::url_seed_alert * a
				= dynamic_cast<libtorrent::url_seed_alert*> (alert)) {
			std::cout << "url_seed_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::hash_failed_alert * a
				= dynamic_cast<libtorrent::hash_failed_alert*> (alert)) {
			std::cout << "hash_failed_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::peer_ban_alert * a
				= dynamic_cast<libtorrent::peer_ban_alert*> (alert)) {
			std::cout << "peer_ban_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::peer_error_alert * a
				= dynamic_cast<libtorrent::peer_error_alert*> (alert)) {
			std::cout << "peer_error_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::invalid_request_alert * a
				= dynamic_cast<libtorrent::invalid_request_alert*> (alert)) {
			std::cout << "invalid_request_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::performance_alert * a
				= dynamic_cast<libtorrent::performance_alert*> (alert)) {
			std::cout << "performance_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::metadata_failed_alert * a
				= dynamic_cast<libtorrent::metadata_failed_alert*> (alert)) {
			std::cout << "metadata_failed_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::metadata_received_alert * a
				= dynamic_cast<libtorrent::metadata_received_alert*> (alert)) {
			std::cout << "metadata_received_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::fastresume_rejected_alert * a
				= dynamic_cast<libtorrent::fastresume_rejected_alert*> (alert)) {
			std::cout << "fastresume_rejected_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::peer_blocked_alert * a
				= dynamic_cast<libtorrent::peer_blocked_alert*> (alert)) {
			std::cout << "peer_blocked_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::storage_moved_alert * a
				= dynamic_cast<libtorrent::storage_moved_alert*> (alert)) {
			std::cout << "storage_moved_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::torrent_paused_alert * a
				= dynamic_cast<libtorrent::torrent_paused_alert*> (alert)) {
			std::cout << "torrent_paused_alert" << std::endl;
			libtorrent::torrent_handle handle = a->handle;
			const char* sha1 = getSha1String(handle.info_hash());
			torrentPausedCallback(sha1, message.c_str());
		} else if (libtorrent::torrent_resumed_alert * a
				= dynamic_cast<libtorrent::torrent_resumed_alert*> (alert)) {
			std::cout << "torrent_resumed_alert" << std::endl;
			libtorrent::torrent_handle handle = a->handle;
			const char* sha1 = getSha1String(handle.info_hash());
			torrentResumedCallback(sha1, message.c_str());
		} else if (libtorrent::save_resume_data_alert * a
				= dynamic_cast<libtorrent::save_resume_data_alert*> (alert)) {
			std::cout << "save_resume_data_alert" << std::endl;
			alertCallback(message.c_str());
		} else if (libtorrent::save_resume_data_failed_alert * a
				= dynamic_cast<libtorrent::save_resume_data_failed_alert*> (alert)) {
			std::cout << "save_resume_data_failed_alert" << std::endl;
			alertCallback(message.c_str());
		} else {
			std::cout << "alert" << std::endl;
			alertCallback(message.c_str());
		}

		alerts = s.pop_alert();
	}
}

void Alert(const char* message) {
	std::cout << message << std::endl;
}

void Completed(const char* sha1, const char* message) {
	std::cout << "Complete: " << sha1 << " - " << message << std::endl;
}

void Resumed(const char* sha1, const char* message) {
	std::cout << "Resumed: " << sha1 << " - " << message << std::endl;
}

void Paused(const char* sha1, const char* message) {
	std::cout << "Resumed: " << sha1 << " - " << message << std::endl;
}

extern "C" int abort_torrent() {
	s.abort();
	return 0;
}

int main(int argc, char* argv[]) {
	try {
		init("/home/pvertenten/Desktop");
		const char
				* id =
						add_torrent(
								"id",
								"/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent");

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
			get_alerts(Alert, Completed, Paused, Resumed);
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

