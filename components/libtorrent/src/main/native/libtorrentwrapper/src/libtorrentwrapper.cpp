#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <memory>

#include "libtorrent/session.hpp"
#include  "boost/shared_ptr.hpp"
#include "libtorrent/alert.hpp"
#include "libtorrent/alert_types.hpp"
#include <dlfcn.h>

libtorrent::session s;
std::string savePath;
std::map<std::string, libtorrent::torrent_handle> handles;

extern "C" int init(char* path) {
	std::string newPath(path);
	savePath = newPath;
	s.set_alert_mask(0xffffffff);
	s.listen_on(std::make_pair(6881, 6889));

	return 0;
}

extern "C" int add_torrent(char* id, char* path) {
	libtorrent::add_torrent_params p;
	p.save_path = savePath;
	p.ti = new libtorrent::torrent_info(path);
	libtorrent::torrent_handle h = s.add_torrent(p);

	handles.insert(std::make_pair(std::string(id), h));
	return 0;
}

extern "C" int pause_torrent(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	h.pause();
	return 0;
}

extern "C" bool is_torrent_paused(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	return h.is_paused();
}

extern "C" bool is_torrent_seed(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	return h.is_seed();
}

extern "C" const char* get_torrent_name(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	return h.name().c_str();
}

extern "C" bool is_torrent_finished(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	return h.is_finished();
}

extern "C" bool is_torrent_valid(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	std::cout << "is_torrent_valid: " << h.is_valid() << std::endl;
	return h.is_valid();
}

extern "C" int resume_torrent(char* id) {
	libtorrent::torrent_handle h = handles[std::string(id)];
	h.resume();
	return 0;
}

struct torrent_s {
	long total_done;
	float download_rate;
	int num_peers;
	int state;
	float progress;
};

extern "C" void* get_torrent_status(char* id, void* stat) {
	struct torrent_s* s = (struct torrent_s *) stat;
	libtorrent::torrent_handle h = handles[std::string(id)];
	libtorrent::torrent_status status = h.status();

	float download_rate = status.download_rate;
	long total_done = status.total_done;
	int num_peers = status.num_peers;
	int state = status.state;
	float progress = status.progress;

	s->total_done = total_done;
	s->download_rate = download_rate;
	s->num_peers = num_peers;
	s->state = state;
	s->progress = progress;

	return s;
}

extern "C" void get_alerts(void(*alertCallback)(const char*),
		void(*torrentFinishedCallback)(const char*, int)) {
	std::auto_ptr<libtorrent::alert> alerts;

	alerts = s.pop_alert();

	while (alerts.get()) {
		libtorrent::alert* alert = alerts.get();
		std::string message = alert->message();
		int alertCategory = alert->category();
		if (libtorrent::torrent_finished_alert * a
				= dynamic_cast<libtorrent::torrent_finished_alert*> (alert)) {
			std::cout << "torrent_finished_alert" << std::endl;
			torrentFinishedCallback(message.c_str(), 1);
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
			alertCallback(message.c_str());
		} else if (libtorrent::torrent_resumed_alert * a
				= dynamic_cast<libtorrent::torrent_resumed_alert*> (alert)) {
			std::cout << "torrent_resumed_alert" << std::endl;
			alertCallback(message.c_str());
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

void TestFunc(const char* message) {
	std::cout << message << std::endl;
}

void TestFunc2(const char* message, int i) {
	std::cout << "Complete: " << message << std::endl;
}

extern "C" int abort_torrent() {
	s.abort();
	return 0;
}

int main(int argc, char* argv[]) {
	try {
		char* id = "id";
		init("/home/pvertenten/Desktop");
		add_torrent(
				id,
				"/home/pvertenten/Desktop/wndw - wireless networking in the developing world.torrent");

		int count = 1;
		bool paused = false;
		while (true) {
			std::cout << "paused: " << is_torrent_paused("id") << std::endl;
			std::cout << "finished: " << is_torrent_finished("id") << std::endl;
			std::cout << "valid: " << is_torrent_valid("id") << std::endl;
			if (count % 30 == 0) {
				if (paused) {
					resume_torrent("id");
					paused = false;
				} else {
					pause_torrent("id");
					paused = true;
				}
			}
			get_alerts(TestFunc, TestFunc2);
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

