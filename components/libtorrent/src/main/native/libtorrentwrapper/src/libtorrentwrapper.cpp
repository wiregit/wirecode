#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <memory>
#include <string.h>

#include "libtorrent/utf8.hpp"
#include "libtorrent/config.hpp"
#include "libtorrent/session.hpp"
#include "libtorrent/peer_info.hpp"
#include  "boost/shared_ptr.hpp"
#include "libtorrent/alert.hpp"
#include "libtorrent/alert_types.hpp"
#include "libtorrent/peer_id.hpp"
#include <boost/filesystem/path.hpp>
#include "libtorrent/size_type.hpp"

#include "libtorrent/entry.hpp"
#include "libtorrent/bencode.hpp"
#include "libtorrent/session.hpp"
#include "libtorrent/identify_client.hpp"
#include "libtorrent/alert_types.hpp"
#include "libtorrent/ip_filter.hpp"
#include "libtorrent/magnet_uri.hpp"
#include "libtorrent/bitfield.hpp"
#include "libtorrent/file.hpp"

#include "libtorrent/socket.hpp"
#include <libtorrent/extensions/metadata_transfer.hpp>
#include <libtorrent/extensions/ut_metadata.hpp>
#include <libtorrent/extensions/ut_pex.hpp>
#include <libtorrent/extensions/smart_ban.hpp>

using libtorrent::asio::ip::tcp;

libtorrent::session* session = 0;

typedef libtorrent::big_number sha1_hash;

#ifdef NO_ERROR
#define EXTERN_RET int
#define wTHROW(x) return 0;
#else
#define EXTERN_RET wrapper_exception*
#define wTHROW(x) if (last_error) { delete last_error; last_error=0;} last_error = x; return last_error;
#endif

#define EXCEPTION_UNKNOWN_RETHROWN 0
#define EXCEPTION_RETHROWN 1
#define EXCEPTION_MANUALLY_THROWN 2

#define EXTERN_TRY_CONTAINER_BEGIN try {

#define EXTERN_TRY_CONTAINER_END \
} catch (std::exception e) \
{	wTHROW(new wrapper_exception(e)); \
} catch (...) \
{	wTHROW(new wrapper_exception()); \
} \
return 0;

#define WIDE_PATH(x) boost::filesystem::path(libtorrent::wchar_utf8(x))

struct wrapper_exception {
	int type;
	const char* message;

	wrapper_exception(std::exception e) {
		this->type = EXCEPTION_RETHROWN;
		this->message = e.what();
	}

	wrapper_exception(char* message) {
		this->type = EXCEPTION_MANUALLY_THROWN;
		this->message = message;
	}

	wrapper_exception() {
		this->type = EXCEPTION_UNKNOWN_RETHROWN;
		this->message = "unfilled";
	}
};
wrapper_exception* last_error = 0;

struct wrapper_torrent_status {
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
	const char* error;
};

struct wrapper_alert_info {
	int category;
	char* sha1;
	const char* message;
	const char* data;

	wrapper_alert_info() {
		sha1 = new char[41];
		sha1[0] = 0;
		category = 0;
		message = 0;
		data = 0;
	}

	~wrapper_alert_info() {
		delete[] sha1;
	}
};

void getString(std::stringstream& oss, char* heap) {
	std::string str = oss.str();
	const char* chars = str.c_str();

	//memcpy(&heap, &chars, str.length()+1);
	for (int i = 0; i < str.length(); i++) {
		heap[i] = chars[i];
	}
	heap[str.length()] = '\0';
}

void getIntString(int num, char* heap) {
	std::stringstream oss;
	oss << num;
	getString(oss, heap);
}

void getSizeTypeString(libtorrent::size_type size, char* heap) {
	std::stringstream oss;
	oss << size;
	getString(oss, heap);
}

void getSha1String(libtorrent::sha1_hash sha1, char* heap) {
	std::stringstream oss;
	oss << sha1;
	getString(oss, heap);
}

sha1_hash getSha1Hash(const char* sha1String) {
	sha1_hash sha1;
	std::stringstream oss;
	oss << sha1String;
	oss >> sha1;
	return sha1;
}

void get_wrapper_torrent_status(libtorrent::torrent_handle handle,
		wrapper_torrent_status* stats) {
	libtorrent::torrent_status status = handle.status();

	char* total_done = new char[20];
	char* total_download = new char[20];
	char* total_upload = new char[20];
	char* error = strdup(status.error.c_str());

	float download_rate = status.download_rate;
	float upload_rate = status.upload_rate;

	getSizeTypeString(status.total_done, total_done);
	getSizeTypeString(status.total_download, total_download);
	getSizeTypeString(status.total_upload, total_upload);

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
	stats->error = error;

}

libtorrent::torrent_handle findTorrentHandle(const char* sha1String) {
	sha1_hash sha1 = getSha1Hash(sha1String);
	libtorrent::torrent_handle torrent_handle = session->find_torrent(sha1);
	return torrent_handle;
}

void process_save_resume_data_alert(libtorrent::torrent_handle handle,
		libtorrent::save_resume_data_alert const* alert,
		wrapper_alert_info* alertInfo) {

	bool seed = handle.is_seed();
#ifdef LIME_DEBUG
	std::cout << "save_resume_data_alert: is_seed=" << seed << std::endl;
#endif

	if (!seed) {
		std::string resume_data_file = handle.get_torrent_info().name()
				+ ".fastresume";
		boost::filesystem::path path(handle.save_path() / resume_data_file);
		alertInfo->data = path.file_string().c_str();

#ifdef LIME_DEBUG
		std::cout << "(to " << alertInfo->data << ')' << std::endl;
#endif

		boost::filesystem::ofstream out(path, std::ios_base::binary);
		out.unsetf(std::ios_base::skipws);
		libtorrent::bencode(std::ostream_iterator<char>(out),
				*alert->resume_data);
	}
}

void process_alert(libtorrent::alert const* alert,
		wrapper_alert_info* alertInfo) {

#ifdef LIME_DEBUG
	std::cout << "process alert" << std::endl;
#endif
	alertInfo->category = alert->category();
	alertInfo->message = alert->message().c_str();

	libtorrent::torrent_alert const* torrentAlert;

	if (torrentAlert = dynamic_cast<libtorrent::torrent_alert const*> (alert)) {

		libtorrent::torrent_handle handle = torrentAlert->handle;

		if (handle.is_valid()) {

			getSha1String(handle.info_hash(), alertInfo->sha1);

			libtorrent::save_resume_data_alert const
					* srd_alert =
							dynamic_cast<libtorrent::save_resume_data_alert const*> (alert);
			if (srd_alert) {
				process_save_resume_data_alert(handle, srd_alert, alertInfo);
				return;
			}

			libtorrent::save_resume_data_failed_alert const
					* srdf_alert =
							dynamic_cast<libtorrent::save_resume_data_failed_alert const*> (alert);
			if (srdf_alert) {
#ifdef LIME_DEBUG
				std::cout << "save_resume_data_failed_alert (" << srdf_alert->msg << ")" << std::endl;
#endif
				alertInfo->message = srdf_alert->msg.c_str();
				return;
			}

			libtorrent::fastresume_rejected_alert const
					* fra_alert =
							dynamic_cast<libtorrent::fastresume_rejected_alert const*> (alert);
			if (fra_alert) {
#ifdef LIME_DEBUG
				std::cout << "fastresume_rejected_alert (" << fra_alert->msg << ")" << std::endl;
#endif
				alertInfo->message = fra_alert->msg.c_str();
				return;
			}
		}
	}
}

// Ported from http://www.rasterbar.com/products/libtorrent/manual.html#save-resume-data
extern "C" EXTERN_RET freeze_and_save_all_fast_resume_data(
		void(*alertCallback)(void*)) {
	EXTERN_TRY_CONTAINER_BEGIN;
		int num_resume_data = 0;

		std::vector<libtorrent::torrent_handle> handles = session->get_torrents();
		session->pause();

		for (std::vector<libtorrent::torrent_handle>::iterator i =
				handles.begin(); i != handles.end(); ++i) {
			libtorrent::torrent_handle& h = *i;
			if (!h.has_metadata())
				continue;
			if (!h.is_valid())
				continue;

			h.save_resume_data();
			++num_resume_data;
#ifdef LIME_DEBUG
			std::cout << "num_resume: " << num_resume_data << std::endl;
#endif
		}

		while (num_resume_data > 0)

		{
#ifdef LIME_DEBUG
			std::cout << "waiting for resume: " << num_resume_data << std::endl;
#endif

			libtorrent::alert const* alert = session->wait_for_alert(
					libtorrent::seconds(10));

			// if we don't get an alert within 10 seconds, abort
			if (alert == 0)
				break;

			std::auto_ptr<libtorrent::alert> holder = session->pop_alert();

			wrapper_alert_info* alertInfo = new wrapper_alert_info();
			process_alert(alert, alertInfo);
			alertCallback(alertInfo);

			if (alertInfo->data) {
#ifdef LIME_DEBUG
				std::cout << "resume_found: " << std::endl;
#endif
				--num_resume_data;
			}
		}

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET init() {
	EXTERN_TRY_CONTAINER_BEGIN;
		session = new libtorrent::session;
		session->set_alert_mask(0xffffffff);
		session->listen_on(std::make_pair(6881, 6889));
		session->add_extension(&libtorrent::create_metadata_plugin);
		session->add_extension(&libtorrent::create_ut_metadata_plugin);
		session->add_extension(&libtorrent::create_ut_pex_plugin);
		session->add_extension(&libtorrent::create_smart_ban_plugin);
		session->start_upnp();
		session->start_natpmp();

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET abort_torrents() {
	EXTERN_TRY_CONTAINER_BEGIN;

		session->pause();
		session->stop_upnp();
		session->stop_natpmp();
		session->abort();
		delete session;

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET move_torrent(const char* id, const char* path) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		std::string newPath(path);
		h.move_storage(newPath);

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET add_torrent(char* sha1String, char* trackerURI,
		wchar_t* torrentPath, wchar_t* savePath, wchar_t* fastResumePath) {
	EXTERN_TRY_CONTAINER_BEGIN;

#ifdef LIME_DEBUG
		std::cout << "adding torrent" << std::endl;
		std::cout << "sha1String" << sha1String << std::endl;
		std::cout << "trackerURI" << trackerURI << std::endl;
		std::cout << "torrentPath: " << torrentPath << std::endl;
		std::cout << "resumeFilePath: " << fastResumePath << std::endl;
#endif

		sha1_hash sha1 = getSha1Hash(sha1String);

		libtorrent::add_torrent_params torrent_params;
		torrent_params.save_path = WIDE_PATH(savePath);
		torrent_params.info_hash = sha1;
		torrent_params.tracker_url = trackerURI;
		torrent_params.auto_managed = false;

		std::vector<char> resume_buf;

		if (torrentPath) {
			boost::filesystem::ifstream torrent_file(
					WIDE_PATH(torrentPath),
					std::ios_base::binary);
			if (!torrent_file.fail()) {
				torrent_params.ti = new libtorrent::torrent_info(
					WIDE_PATH(torrentPath));
			}
		}

		if (fastResumePath) {
			boost::filesystem::ifstream resume_file(
					WIDE_PATH(fastResumePath),
					std::ios_base::binary);

			if (!resume_file.fail()) {
				resume_file.unsetf(std::ios_base::skipws);

				std::istream_iterator<char> ios_iter;
				std::istream_iterator<char> iter(resume_file);

				std::copy(iter, ios_iter, std::back_inserter(resume_buf));

				torrent_params.resume_data = &resume_buf;
			}
		}

		libtorrent::torrent_handle h = session->add_torrent(torrent_params);

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET pause_torrent(const char* id) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		h.pause();

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET remove_torrent(const char* id) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		session->remove_torrent(h);

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET resume_torrent(const char* id) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		h.resume();

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET get_torrent_status(const char* id, void* stat) {
	EXTERN_TRY_CONTAINER_BEGIN;

		struct wrapper_torrent_status* stats =
				(struct wrapper_torrent_status *) stat;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		get_wrapper_torrent_status(h, stats);

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET signal_fast_resume_data_request(const char* id) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		if (h.has_metadata()) {
			h.save_resume_data();
		}

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET clear_error_and_retry(const char* id) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);
		h.clear_error();

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET get_num_viewable_peers(const char* id, char* num_peers) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);

		libtorrent::torrent_status s = h.status();

		if (s.state == libtorrent::torrent_status::seeding)
			return 0;

		std::vector<libtorrent::peer_list_entry> peers;

		try {
			h.get_full_peer_list(peers);
		} catch (libtorrent::invalid_handle e) {
			return 0;
		} catch (std::exception e) {
			return 0;
		}

		int num = peers.size();

		// Limit the maximum number that can be viewed to 300
		if (num > 300)
			num = 300;

		getIntString(num, num_peers);

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET get_peers(const char* id, int buffer_len, char* data) {
	EXTERN_TRY_CONTAINER_BEGIN;

		libtorrent::torrent_handle h = findTorrentHandle(id);

		std::vector<libtorrent::peer_list_entry> peers;
		h.get_full_peer_list(peers);

		int pos = 0;

		std::vector<libtorrent::peer_list_entry>::iterator iter = peers.begin();

		while (iter != peers.end()) {

			std::string address = iter->ip.address().to_string();
			int len = address.length();

#ifdef LIME_DEBUG
			std::cout << "peer:" << address << std::endl;
#endif

			if (len + pos > buffer_len)
				break;

			for (int i = 0; i < len; i++) {
				data[pos + i] = address[i];
			}

			pos += len;
			data[pos++] = ';';
			data[pos] = '\0';
			++iter;
		}

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET free_torrent_status(wrapper_torrent_status* info) {
	EXTERN_TRY_CONTAINER_BEGIN;

		delete[] info->total_done;
		delete[] info->total_download;
		delete[] info->total_upload;
		delete[] info->error;

	EXTERN_TRY_CONTAINER_END;
}

extern "C" EXTERN_RET get_alerts(void(*alertCallback)(void*)) {
	EXTERN_TRY_CONTAINER_BEGIN;

		std::auto_ptr<libtorrent::alert> alerts;

		alerts = session->pop_alert();

		while (alerts.get()) {
			libtorrent::alert* alert = alerts.get();

			wrapper_alert_info* alertInfo = new wrapper_alert_info();

			process_alert(alert, alertInfo);
			alertCallback(alertInfo);

			delete alertInfo;

			alerts = session->pop_alert();
		}

	EXTERN_TRY_CONTAINER_END;
}
