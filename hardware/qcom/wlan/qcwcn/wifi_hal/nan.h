/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __NAN_H__
#define __NAN_H__

#include "wifi_hal.h"

#ifdef __cplusplus
extern "C"
{
#endif /* __cplusplus */

/*****************************************************************************
 * Neighbour Aware Network Service Structures and Functions
 *****************************************************************************/

#ifndef PACKED
#define PACKED  __attribute__((packed))
#endif

/*
  Definitions
  All multi-byte fields within all NAN protocol stack messages are assumed to be in Little Endian order.
*/

typedef int NanVersion;

#define NAN_MAC_ADDR_LEN                6
#define NAN_COUNTRY_STRING_LEN          3
#define NAN_MAJOR_VERSION               2
#define NAN_MINOR_VERSION               0
#define NAN_MICRO_VERSION               0
#define NAN_MAX_SOCIAL_CHANNEL 3

/* NAN Maximum Lengths */
#define NAN_MAX_SERVICE_NAME_LEN                255
#define NAN_MAX_MATCH_FILTER_LEN                255
#define NAN_MAX_SERVICE_SPECIFIC_INFO_LEN       1024
#define NAN_MAX_VSA_DATA_LEN                    1024
#define NAN_MAX_MESH_DATA_LEN                   32
#define NAN_MAX_CLUSTER_ATTRIBUTE_LEN           255
#define NAN_MAX_SUBSCRIBE_MAX_ADDRESS           42
#define NAN_MAX_FAM_CHANNELS                    32

/*
  Definition of various NanRequestType
*/
typedef enum {
    NAN_REQUEST_ENABLE                  =0,
    NAN_REQUEST_DISABLE                 =1,
    NAN_REQUEST_PUBLISH                 =2,
    NAN_REQUEST_PUBLISH_CANCEL          =3,
    NAN_REQUEST_TRANSMIT_FOLLOWUP       =4,
    NAN_REQUEST_SUBSCRIBE               =5,
    NAN_REQUEST_SUBSCRIBE_CANCEL        =6,
    NAN_REQUEST_STATS                   =7,
    NAN_REQUEST_CONFIG                  =8,
    NAN_REQUEST_TCA                     =9,
    NAN_REQUEST_BEACON_SDF_PAYLOAD      =10,
    NAN_REQUEST_LAST                    =0xFFFF
} NanRequestType;

/*
  Definition of various NanResponseType
*/
typedef enum {
    NAN_RESPONSE_ENABLED                =0,
    NAN_RESPONSE_DISABLED               =1,
    NAN_RESPONSE_PUBLISH                =2,
    NAN_RESPONSE_PUBLISH_CANCEL         =3,
    NAN_RESPONSE_TRANSMIT_FOLLOWUP      =4,
    NAN_RESPONSE_SUBSCRIBE              =5,
    NAN_RESPONSE_SUBSCRIBE_CANCEL       =6,
    NAN_RESPONSE_STATS                  =7,
    NAN_RESPONSE_CONFIG                 =8,
    NAN_RESPONSE_TCA                    =9,
    NAN_RESPONSE_ERROR                  =10,
    NAN_RESPONSE_BEACON_SDF_PAYLOAD     =11,
    NAN_RESPONSE_UNKNOWN                =0xFFFF
} NanResponseType;

/*
  Definition of various NanIndication(events)
*/
typedef enum {
    NAN_INDICATION_PUBLISH_REPLIED         =0,
    NAN_INDICATION_PUBLISH_TERMINATED      =1,
    NAN_INDICATION_MATCH                   =2,
    NAN_INDICATION_UNMATCH                 =3,
    NAN_INDICATION_SUBSCRIBE_TERMINATED    =4,
    NAN_INDICATION_DE_EVENT                =5,
    NAN_INDICATION_FOLLOWUP                =6,
    NAN_INDICATION_DISABLED                =7,
    NAN_INDICATION_TCA                     =8,
    NAN_INDICATION_BEACON_SDF_PAYLOAD      =9,
    NAN_INDICATION_UNKNOWN                 =0xFFFF
} NanIndicationType;


/* NAN Publish Types */
typedef enum {
    NAN_PUBLISH_TYPE_UNSOLICITED = 0,
    NAN_PUBLISH_TYPE_SOLICITED,
    NAN_PUBLISH_TYPE_UNSOLICITED_SOLICITED,
    NAN_PUBLISH_TYPE_LAST,
} NanPublishType;

/* NAN Transmit Priorities */
typedef enum {
    NAN_TX_PRIORITY_LOW = 0,
    NAN_TX_PRIORITY_NORMAL,
    NAN_TX_PRIORITY_HIGH,
    NAN_TX_PRIORITY_LAST
} NanTxPriority;

/* NAN Statistics Request ID Codes */
typedef enum
{
    NAN_STATS_ID_FIRST = 0,
    NAN_STATS_ID_DE_PUBLISH = NAN_STATS_ID_FIRST,
    NAN_STATS_ID_DE_SUBSCRIBE,
    NAN_STATS_ID_DE_MAC,
    NAN_STATS_ID_DE_TIMING_SYNC,
    NAN_STATS_ID_DE_DW,
    NAN_STATS_ID_DE,
    NAN_STATS_ID_LAST
} NanStatsId;

/* NAN Protocol Event ID Codes */
typedef enum
{
    NAN_EVENT_ID_FIRST = 0,
    NAN_EVENT_ID_STA_MAC_ADDR = NAN_EVENT_ID_FIRST,
    NAN_EVENT_ID_STARTED_CLUSTER,
    NAN_EVENT_ID_JOINED_CLUSTER,
    NAN_EVENT_ID_LAST
} NanEventId;

/* TCA IDs */
typedef enum
{
    NAN_TCA_ID_FIRST = 0,
    NAN_TCA_ID_CLUSTER_SIZE = NAN_TCA_ID_FIRST,
    NAN_TCA_ID_LAST
} NanTcaId;

/*
  Various NAN Protocol Response code
*/
#ifndef NAN_2_0
typedef enum
{
    /* NAN Protocol Response Codes */
    NAN_STATUS_SUCCESS = 0,
    NAN_STATUS_TIMEOUT,
    NAN_STATUS_DE_FAILURE,
    NAN_STATUS_INVALID_MSG_VERSION,
    NAN_STATUS_INVALID_MSG_LEN,
    NAN_STATUS_INVALID_MSG_ID,
    NAN_STATUS_INVALID_HANDLE,
    NAN_STATUS_NO_SPACE_AVAILABLE,
    NAN_STATUS_INVALID_PUBLISH_TYPE,
    NAN_STATUS_INVALID_TX_TYPE,
    NAN_STATUS_INVALID_MATCH_ALGORITHM,
    NAN_STATUS_DISABLE_IN_PROGRESS,
    NAN_STATUS_INVALID_TLV_LEN,
    NAN_STATUS_INVALID_TLV_TYPE,
    NAN_STATUS_MISSING_TLV_TYPE,
    NAN_STATUS_INVALID_TOTAL_TLVS_LEN,
    NAN_STATUS_INVALID_MATCH_HANDLE,
    NAN_STATUS_INVALID_TLV_VALUE,
    NAN_STATUS_INVALID_TX_PRIORITY,
    NAN_STATUS_INVALID_TCA_ID,
    NAN_STATUS_INVALID_STATS_ID,

    /* NAN Configuration Response codes */
    NAN_STATUS_INVALID_RSSI_CLOSE_VALUE = 128,
    NAN_STATUS_INVALID_RSSI_MEDIUM_VALUE,
    NAN_STATUS_INVALID_HOP_COUNT_LIMIT,
    NAN_STATUS_INVALID_CLUSTER_JOIN_COUNT,
    NAN_STATUS_INVALID_MIN_WAKE_DW_DURATION_VALUE,
    NAN_STATUS_INVALID_OFDM_DATA_RATE_VALUE,
    NAN_STATUS_INVALID_RANDOM_FACTOR_UPDATE_TIME_VALUE,
    NAN_STATUS_INVALID_MASTER_PREFERENCE_VALUE,
    NAN_STATUS_INVALID_EARLY_DW_WAKE_INTERVAL_VALUE,
    NAN_STATUS_INVALID_LOW_CLUSTER_ID_VALUE,
    NAN_STATUS_INVALID_HIGH_CLUSTER_ID_VALUE,
    NAN_STATUS_INVALID_INITIAL_SCAN_PERIOD,
    NAN_STATUS_INVALID_ONGOING_SCAN_PERIOD,
    NAN_STATUS_INVALID_RSSI_PROXIMITY_VALUE,
    NAN_STATUS_INVALID_BACKGROUND_SCAN_PERIOD,
    NAN_STATUS_INVALID_SCAN_CHANNEL
} NanStatusType;
#else /* NAN_2_0 */
typedef enum
{
    /* NAN Protocol Response Codes */
    NAN_STATUS_SUCCESS = 0,
    NAN_STATUS_TIMEOUT = 1,
    NAN_STATUS_DE_FAILURE = 2,
    NAN_STATUS_INVALID_MSG_VERSION = 3,
    NAN_STATUS_INVALID_MSG_LEN = 4,
    NAN_STATUS_INVALID_MSG_ID = 5,
    NAN_STATUS_INVALID_HANDLE = 6,
    NAN_STATUS_NO_SPACE_AVAILABLE = 7,
    NAN_STATUS_INVALID_PUBLISH_TYPE = 8,
    NAN_STATUS_INVALID_TX_TYPE = 9,
    NAN_STATUS_INVALID_MATCH_ALGORITHM = 10,
    NAN_STATUS_DISABLE_IN_PROGRESS = 11,
    NAN_STATUS_INVALID_TLV_LEN = 12,
    NAN_STATUS_INVALID_TLV_TYPE = 13,
    NAN_STATUS_MISSING_TLV_TYPE = 14,
    NAN_STATUS_INVALID_TOTAL_TLVS_LEN = 15,
    NAN_STATUS_INVALID_MATCH_HANDLE= 16,
    NAN_STATUS_INVALID_TLV_VALUE = 17,
    NAN_STATUS_INVALID_TX_PRIORITY = 18,
    NAN_STATUS_INVALID_CONNECTION_MAP = 19,
    /* 20-4095 Reserved */

    /* NAN Configuration Response codes */
    NAN_STATUS_INVALID_RSSI_CLOSE_VALUE = 4096,
    NAN_STATUS_INVALID_RSSI_MIDDLE_VALUE = 4097,
    NAN_STATUS_INVALID_HOP_COUNT_LIMIT = 4098,
    NAN_STATUS_INVALID_MASTER_PREFERENCE_VALUE = 4099,
    NAN_STATUS_INVALID_LOW_CLUSTER_ID_VALUE = 4100,
    NAN_STATUS_INVALID_HIGH_CLUSTER_ID_VALUE = 4101,
    NAN_STATUS_INVALID_BACKGROUND_SCAN_PERIOD = 4102,
    NAN_STATUS_INVALID_RSSI_PROXIMITY_VALUE = 4103,
    NAN_STATUS_INVALID_SCAN_CHANNEL = 4104,
    NAN_STATUS_INVALID_POST_NAN_CONNECTIVITY_CAPABILITIES_BITMAP = 4105,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_NUMCHAN_VALUE = 4106,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_DURATION_VALUE = 4107,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_CLASS_VALUE = 4108,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_CHANNEL_VALUE = 4109,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_AVAILABILITY_INTERVAL_BITMAP_VALUE = 4110,
    NAN_STATUS_INVALID_FURTHER_AVAILABILITY_MAP_MAP_ID = 4111,
    NAN_STATUS_INVALID_POST_NAN_DISCOVERY_CONN_TYPE_VALUE = 4112,
    NAN_STATUS_INVALID_POST_NAN_DISCOVERY_DEVICE_ROLE_VALUE = 4113,
    NAN_STATUS_INVALID_POST_NAN_DISCOVERY_DURATION_VALUE = 4114,
    NAN_STATUS_INVALID_POST_NAN_DISCOVERY_BITMAP_VALUE = 4115,
    NAN_STATUS_MISSING_FUTHER_AVAILABILITY_MAP = 4116,
    NAN_STATUS_INVALID_BAND_CONFIG_FLAGS = 4117,
    /* 4118 RESERVED */
} NanStatusType;
#endif /* NAN_2_0 */
/*
  Various NAN Terminated Indication Code
*/
typedef enum
{
    NAN_TERMINATED_REASON_INVALID = 0,
    NAN_TERMINATED_REASON_TIMEOUT,
    NAN_TERMINATED_REASON_USER_REQUEST,
    NAN_TERMINATED_REASON_FAILURE,
    NAN_TERMINATED_REASON_COUNT_REACHED,
    NAN_TERMINATED_REASON_DE_SHUTDOWN,
    NAN_TERMINATED_REASON_DISABLE_IN_PROGRESS,
    NAN_TERMINATED_REASON_POST_DISC_ATTR_EXPIRED,
    NAN_TERMINATED_REASON_POST_DISC_LEN_EXCEEDED,
    NAN_TERMINATED_REASON_FURTHER_AVAIL_MAP_EMPTY
} NanTerminatedStatus;

/* NAN Transmit Types */
typedef enum
{
    NAN_TX_TYPE_BROADCAST = 0,
    NAN_TX_TYPE_UNICAST,
    NAN_TX_TYPE_LAST
} NanTxType;

/* NAN Subscribe Type Bit */
#define NAN_SUBSCRIBE_TYPE_PASSIVE  0
#define NAN_SUBSCRIBE_TYPE_ACTIVE   1

/* NAN Service Response Filter Attribute Bit */
#define NAN_SRF_ATTR_BLOOM_FILTER       0
#define NAN_SRF_ATTR_PARTIAL_MAC_ADDR   1

/* NAN Service Response Filter Include Bit */
#define NAN_SRF_INCLUDE_DO_NOT_RESPOND  0
#define NAN_SRF_INCLUDE_RESPOND         1

/* NAN Match Algorithms */
typedef enum
{
    NAN_MATCH_ALG_FIRST = 0,
    NAN_MATCH_ALG_MATCH_ONCE = NAN_MATCH_ALG_FIRST,
    NAN_MATCH_ALG_MATCH_CONTINUOUS,
    NAN_MATCH_ALG_MATCH_NEVER,
    NAN_MATCH_ALG_LAST
} NanMatchAlg;

/* NAN Header */
typedef struct {
    /*
    16-bit quantity which is allocated by the FW.
    Pass the Handle as 0xFFFF if the Host would like to set up a new
    Publish/Subscribe and the FW will pass back a valid handle in response msg.
    To update an already opened Publish/Subscribe Host can pass a Handle
    which has already been allocated by the FW.
    */
    u16 handle;

    /*
    16-bit quantity which is allocated in 2 contexts.  For all Request
    messages the TransactionId is allocated by the Service Layer and
    passed down to the DE.  In all Indication messages the TransactionId
    field is allocated by the DE.  There is no correlation between the
    TransactionIds allocated by the Service Layer and those allocated by the DE
    */
    u16 transaction_id;
} NanHeader;

/*
  Host can send Vendor specific attributes which the Discovery Engine can
  enclose in Beacons and/or Service Discovery frames transmitted.
  Below structure is used to populate that.
*/
typedef struct {
    /*
       0 = transmit only in the next discovery window
       1 = transmit in next 16 discovery window
    */
    u8 payload_transmit_flag;
    /*
       Below flags will determine in which all frames
       the vendor specific attributes should be included
    */
    u8 tx_in_discovery_beacon;
    u8 tx_in_sync_beacon;
    u8 tx_in_service_discovery;
    /* Organizationally Unique Identifier */
    u32 vendor_oui;
    /*
       vendor specific attribute to be transmitted
       vsa_len : Length of the vsa data.
     */
    u32 vsa_len;
    u8 vsa[NAN_MAX_VSA_DATA_LEN];
} NanTransmitVendorSpecificAttribute;


/*
  Discovery Engine will forward any Vendor Specific Attributes
  which it received as part of this structure.
*/
/* Mask to determine on which frames attribute was received */
#define RX_DISCOVERY_BEACON_MASK  0x00
#define RX_SYNC_BEACON_MASK       0x01
#define RX_SERVICE_DISCOVERY_MASK  0x02
typedef struct {
    /*
       Frames on which this vendor specific attribute
       was received. Mask defined above
    */
    u8 vsa_received_on;
    /* Organizationally Unique Identifier */
    u32 vendor_oui;
    /* vendor specific attribute */
    u32 attr_len;
    u8 vsa[NAN_MAX_VSA_DATA_LEN];
} NanReceiveVendorSpecificAttribute;

/* Discovery engine cluster state*/
typedef enum {
    NAN_NORMAL_OPERATION = 0,
    NAN_NON_MASTER_NON_SYNC = 1,
    NAN_NON_MASTER_SYNC = 2,
    NAN_MASTER = 3,
    NAN_ANCHOR_MASTER = 4
} NanDebugDEClusterState;

/*
   NAN Beacon SDF Payload Received structure
   Discovery engine sends the details of received Beacon or
   Service Discovery Frames as part of this structure.
*/
typedef struct {
    /* Frame data */
    u32 frame_len;
    u8 frame_data[NAN_MAX_VSA_DATA_LEN];
} NanBeaconSdfPayloadReceive;

/*
  Host can set the Periodic scan parameters for each of the
  3(6, 44, 149) Social channels. Only these channels are allowed
  any other channels are rejected
*/
#define  MAX_SOCIAL_CHANNELS    3
typedef enum
{
    NAN_CHANNEL_6 = 0,
    NAN_CHANNEL_44 = 1,
    NAN_CHANNEL_149 = 2
} NanChannelIndex;

/*
   Structure to set the Social Channel Scan parameters
   passed as part of NanEnableRequest/NanConfigRequest
*/
typedef struct {
    /*
       Dwell time of each social channel in milliseconds
       NanChannelIndex corresponds to the respective channel
       If time set to 0 then the FW default time will be used.
    */
    u8 dwell_time[MAX_SOCIAL_CHANNELS];

    /*
       Scan period of each social channel in seconds
       NanChannelIndex corresponds to the respective channel
       If time set to 0 then the FW default time will be used.
    */
    u16 scan_period[MAX_SOCIAL_CHANNELS];
} NanSocialChannelScanParams;

/*
  Host can send Post Connectivity Capability attributes
  to be included in Service Discovery frames transmitted
  as part of this structure.
*/
typedef struct {
    /*
       0 = transmit only in the next discovery window
       1 = transmit in next 16 discovery window
    */
    u8 payload_transmit_flag;
    /* 1 - Wifi Direct supported 0 - Not supported */
    u8 is_wfd_supported;
    /* 1 - Wifi Direct Services supported 0 - Not supported */
    u8 is_wfds_supported;
    /* 1 - TDLS supported 0 - Not supported */
    u8 is_tdls_supported;
    /* 1 - IBSS supported 0 - Not supported */
    u8 is_ibss_supported;
    /* 1 - Mesh supported 0 - Not supported */
    u8 is_mesh_supported;
    /*
       1 - NAN Device currently connect to WLAN Infra AP
       0 - otherwise
    */
    u8 wlan_infra_field;
} NanTransmitPostConnectivityCapability;

/*
  Discovery engine providing the post connectivity capability
  received.
*/
typedef struct {
    /* 1 - Wifi Direct supported 0 - Not supported */
    u8 is_wfd_supported;
    /* 1 - Wifi Direct Services supported 0 - Not supported */
    u8 is_wfds_supported;
    /* 1 - TDLS supported 0 - Not supported */
    u8 is_tdls_supported;
    /* 1 - IBSS supported 0 - Not supported */
    u8 is_ibss_supported;
    /* 1 - Mesh supported 0 - Not supported */
    u8 is_mesh_supported;
    /*
       1 - NAN Device currently connect to WLAN Infra AP
       0 - otherwise
    */
    u8 wlan_infra_field;
} NanReceivePostConnectivityCapability;

/*
  Indicates the availability interval duration associated with the
  Availability Intervals Bitmap field
*/
typedef enum {
    NAN_DURATION_16MS = 0,
    NAN_DURATION_32MS = 1,
    NAN_DURATION_64MS = 2
} NanAvailDuration;

/* Further availability per channel information */
typedef struct {
    /* Defined above */
    NanAvailDuration entry_control;
    /*
       1 byte field indicating the frequency band the NAN Device
       will be available as defined in IEEE Std. 802.11-2012
       Annex E Table E-4 Global Operating Classes
    */
    u8 class_val;
    /*
       1 byte field indicating the channel the NAN Device
       will be available.
    */
    u8 channel;
    /*
        Map Id - 4 bit field which identifies the Further
        availability map attribute.
    */
    u8 mapid;
    /*
       divides the time between the beginnings of consecutive Discovery
       Windows of a given NAN cluster into consecutive time intervals
       of equal durations. The time interval duration is specified by
       the Availability Interval Duration subfield of the Entry Control
       field.

       A Nan device that sets the i-th bit of the Availability
       Intervals Bitmap to 1 shall be present during the corresponding
       i-th time interval in the operation channel indicated by the
       Operating Class and Channel Number fields in the same Availability Entry.

       A Nan device that sets the i-th bit of the Availability Intervals Bitmap to
       0 may be present during the corresponding i-th time interval in the operation
       channel indicated by the Operating Class and Channel Number fields in the same
       Availability Entry.

       The size of the Bitmap is dependent upon the Availability Interval Duration
       chosen in the Entry Control Field.  The size can be either 1, 2 or 4 bytes long

       - Duration field is equal to 0, only AIB[0] is valid
       - Duration field is equal to 1, only AIB [0] and AIB [1] is valid
       - Duration field is equal to 2, AIB [0], AIB [1], AIB [2] and AIB [3] are valid
    */
    u32 avail_interval_bitmap;
} NanFurtherAvailabilityChannel;

/*
  Further availability map which can be sent and received from
  Discovery engine
*/
typedef struct {
    /*
       Number of channels indicates the number of channel
       entries which is part of fam
    */
    u8 numchans;
    NanFurtherAvailabilityChannel famchan[NAN_MAX_FAM_CHANNELS];
} NanFurtherAvailabilityMap;

/*
  Host can send Post-Nan Discovery attributes which the Discovery Engine can
  enclose in Service Discovery frames
*/
/* Possible connection types in Post NAN Discovery attributes */
typedef enum {
    NAN_CONN_WLAN_INFRA = 0,
    NAN_CONN_P2P_OPER = 1,
    NAN_CONN_WLAN_IBSS = 2,
    NAN_CONN_WLAN_MESH = 3,
    NAN_CONN_FURTHER_SERVICE_AVAILABILITY = 4,
    NAN_CONN_WLAN_RANGING = 5
} NanConnectionType;

/* Possible device roles in Post NAN Discovery attributes */
typedef enum {
    NAN_WLAN_INFRA_AP = 0,
    NAN_WLAN_INFRA_STA = 1,
    NAN_P2P_OPER_GO = 2,
    NAN_P2P_OPER_DEV = 3,
    NAN_P2P_OPER_CLI = 4
} NanDeviceRole;

/* Structure of Post NAN Discovery attribute */
typedef struct {
    /* Connection type of the host */
    NanConnectionType  type;
    /*
       Device role of the host based on
       the connection type
    */
    NanDeviceRole role;
    /*
       Flag to send the information as a single shot or repeated
       for next 16 discovery windows
       0 - Single_shot
       1 - next 16 discovery windows
    */
    u8 transmit_freq;
    /* Duration of the availability bitmask */
    NanAvailDuration duration;
    /* Availability interval bitmap based on duration */
    u32 avail_interval_bitmap;
    /*
       Mac address depending on the conn type and device role
       --------------------------------------------------
       | Conn Type  |  Device Role |  Mac address Usage  |
       --------------------------------------------------
       | WLAN_INFRA |  AP/STA      |   BSSID of the AP   |
       --------------------------------------------------
       | P2P_OPER   |  GO          |   GO's address      |
       --------------------------------------------------
       | P2P_OPER   |  P2P_DEVICE  |   Address of who    |
       |            |              |   would become GO   |
       --------------------------------------------------
       | WLAN_IBSS  |  NA          |   BSSID             |
       --------------------------------------------------
       | WLAN_MESH  |  NA          |   BSSID             |
       --------------------------------------------------
    */
    u8 addr[NAN_MAC_ADDR_LEN];
    /*
       Mandatory mesh id value if connection type is WLAN_MESH
       Mesh id contains 0-32 octet identifier and should be
       as per IEEE Std.802.11-2012 spec.
    */
    u16 mesh_id_len;
    u8 mesh_id[NAN_MAX_MESH_DATA_LEN];
    /*
       Optional infrastructure SSID if conn_type is set to
       NAN_CONN_WLAN_INFRA
    */
    u16 infrastructure_ssid_len;
    u8 infrastructure_ssid_val[NAN_MAX_MESH_DATA_LEN];
} NanTransmitPostDiscovery;

/*
   Discovery engine providing the structure of Post NAN
   Discovery
*/
typedef struct {
    /* Connection type of the host */
    NanConnectionType  type;
    /*
       Device role of the host based on
       the connection type
    */
    NanDeviceRole role;
    /* Duration of the availability bitmask */
    NanAvailDuration duration;
    /* Availability interval bitmap based on duration */
    u32 avail_interval_bitmap;
    /*
       Map Id - 4 bit field which identifies the Further
       availability map attribute.
    */
    u8 mapid;
    /*
       Mac address depending on the conn type and device role
       --------------------------------------------------
       | Conn Type  |  Device Role |  Mac address Usage  |
       --------------------------------------------------
       | WLAN_INFRA |  AP/STA      |   BSSID of the AP   |
       --------------------------------------------------
       | P2P_OPER   |  GO          |   GO's address      |
       --------------------------------------------------
       | P2P_OPER   |  P2P_DEVICE  |   Address of who    |
       |            |              |   would become GO   |
       --------------------------------------------------
       | WLAN_IBSS  |  NA          |   BSSID             |
       --------------------------------------------------
       | WLAN_MESH  |  NA          |   BSSID             |
       --------------------------------------------------
    */
    u8 addr[NAN_MAC_ADDR_LEN];
    /*
       Mandatory mesh id value if connection type is WLAN_MESH
       Mesh id contains 0-32 octet identifier and should be
       as per IEEE Std.802.11-2012 spec.
    */
    u16 mesh_id_len;
    u8 mesh_id[NAN_MAX_MESH_DATA_LEN];
    /*
       Optional infrastructure SSID if conn_type is set to
       NAN_CONN_WLAN_INFRA
    */
    u16 infrastructure_ssid_len;
    u8 infrastructure_ssid_val[NAN_MAX_MESH_DATA_LEN];
} NanReceivePostDiscovery;

/*
  Enable Request Message Structure
  The NanEnableReq message instructs the Discovery Engine to enter an operational state
*/
typedef struct {
    NanHeader header;
    /* Mandatory parameters below */
    u8 support_5g; /* default = 0 */
    u16 cluster_low; /* default = 0 */
    u16 cluster_high; /* default = 0 */
    /*
       BIT 0 is used to specify to include Service IDs in Sync/Discovery beacons
       0 - Do not include SIDs in any beacons
       1 - Include SIDs in all beacons.
       Rest 7 bits are count field which allows control over the number of SIDs
       included in the Beacon.  0 means to include as many SIDs that fit into
       the maximum allow Beacon frame size
    */
    u8 sid_beacon; /* default = 0x01*/
    u8 rssi_close; /* default = 60 (-60 dBm) */
    u8 rssi_middle; /* default = 70 (-70 dBm) */
    u8 rssi_proximity; /* default = 70 (-70 dBm) */
    u8 hop_count_limit; /* default = 2 */
    u8 random_time; /* default  = 120 (DWs) */
    u8 master_pref; /* default = 0 */
    u8 periodic_scan_interval; /* default = 20 seconds */
    /* TBD: Google specific IE */

    /*
      Optional configuration of Enable request.
      Each of the optional parameters have configure flag which
      determine whether configuration is to be passed or not.
    */
    /*
       Defines 2.4G channel access support
       0 - No Support
       1 - Supported
       If not configured, default value = 1
    */
    u8 config_2dot4g_support;
    u8 support_2dot4g_val;
    /*
       Defines 2.4G channels will be used for sync/discovery beacons
       0 - 2.4G channels not used for beacons
       1 - 2.4G channels used for beacons
       If not configured, default value = 1
    */
    u8 config_2dot4g_beacons;
    u8 beacon_2dot4g_val;
    /*
       Defines 2.4G channels will be used for discovery frames
       0 - 2.4G channels not used for discovery frames
       1 - 2.4G channels used for discovery frames
       If not configured, default value = 1
    */
    u8 config_2dot4g_discovery;
    u8 discovery_2dot4g_val;
    /*
       Defines 5G channels will be used for sync/discovery beacons
       0 - 5G channels not used for beacons
       1 - 5G channels used for beacons
       If not configured, default value = 1
    */
    u8 config_5g_beacons;
    u8 beacon_5g_val;
    /*
       Defines 5G channels will be used for discovery frames
       0 - 5G channels not used for discovery frames
       1 - 5G channels used for discovery frames
       If not configured, default value = 0
    */
    u8 config_5g_discovery;
    u8 discovery_5g_val;
    /*
       1 byte signed quantity which defines the RSSI value in
       dBm for a close by Peer in 5 Ghz channels.
    */
    u8 config_5g_rssi_close;
    u8 rssi_close_5g_val;
    /*
       1 byte signed quantity which defines the RSSI value in
       dBm for a close by Peer in 5 Ghz channels.
    */
    u8 config_5g_rssi_middle;
    u8 rssi_middle_5g_val;
    /*
       1 byte signed quantity which defines the RSSI filter
       threshold.  Any Service Descriptors received above this
       value that are configured for RSSI filtering will be dropped.
    */
    u8 config_5g_rssi_close_proximity;
    u8 rssi_close_proximity_5g_val;
    /*
       2 byte quantity which defines the window size over
       which the “average RSSI” will be calculated over.
    */
    u8 config_rssi_window_size;
    u16 rssi_window_size_val;
    /*
       The 24 bit Organizationally Unique ID + the 8 bit Network Id.
    */
    u8 config_oui;
    u32 oui_val;
    /*
       NAN Interface Address, If not configured the Discovery Engine
       will generate a 6 byte Random MAC.
    */
    u8 config_intf_addr;
    u8 intf_addr_val[NAN_MAC_ADDR_LEN];
    /*
       If set to 1, the Discovery Engine will enclose the Cluster
       Attribute only sent in Beacons in a Vendor Specific Attribute
       and transmit in a Service Descriptor Frame.
    */
    u8 config_cluster_attribute_val;
    /*
       The periodicity in seconds between full scan’s to find any new
       clusters available in the area.  A Full scan should not be done
       more than every 10 seconds and should not be done less than every
       30 seconds.
    */
    u8 config_scan_params;
    NanSocialChannelScanParams scan_params_val;
    /*
       Debugging mode for Discovery engine
    */
    u8 config_debug_flags;
    u64 debug_flags_val;
    /*
       1 byte quantity which forces the Random Factor to a particular
       value for all transmitted Sync/Discovery beacons
    */
    u8 config_random_factor_force;
    u8 random_factor_force_val;
    /*
       1 byte quantity which forces the HC for all transmitted Sync and
       Discovery Beacon NO matter the real HC being received over the
       air.
    */
    u8 config_hop_count_force;
    u8 hop_count_force_val;
} NanEnableRequest;

/*
  Disable Request Message Structure
  The NanDisableReq message instructs the Discovery Engine to exit an operational state.
*/
typedef struct {
    NanHeader header;
} NanDisableRequest;

/*
  Publish Msg Structure
  Message is used to request the DE to publish the Service Name
  using the parameters passed into the Discovery Window
*/
typedef struct {
    NanHeader header;
    u16 ttl; /* how many seconds to run for. 0 means forever until canceled */
    u16 period; /* periodicity of OTA unsolicited publish. Specified in increments of 500 ms */
    u8 replied_event_flag; /* 1= RepliedEventInd needed, 0 = Not needed */
    NanPublishType publish_type;/* 0= unsolicited, solicited = 1, 2= both */
    NanTxType tx_type; /* 0 = broadcast, 1= unicast  if solicited publish */
    u8 publish_count; /* number of OTA Publish, 0 means forever until canceled */
    u16 service_name_len; /* length of service name */
    u8 service_name[NAN_MAX_SERVICE_NAME_LEN];/* UTF-8 encoded string identifying the service */
    /*
       Field which allows the matching behavior to be controlled.
       0 - Match Once
       1 - Match continuous
       2 - Match never
       3 - Reserved
    */
    NanMatchAlg publish_match;

    /*
       Sequence of values which should be conveyed to the Discovery Engine of a
       NAN Device that has invoked a Subscribe method corresponding to this Publish method
    */
    u16 service_specific_info_len;
    u8 service_specific_info[NAN_MAX_SERVICE_SPECIFIC_INFO_LEN];

    /*
       Ordered sequence of <length, value> pairs which specify further response conditions
       beyond the service name used to filter subscribe messages to respond to.
       This is only needed when the PT is set to NAN_SOLICITED or NAN_SOLICITED_UNSOLICITED.
    */
    u16 rx_match_filter_len;
    u8 rx_match_filter[NAN_MAX_MATCH_FILTER_LEN];

    /*
       Ordered sequence of <length, value> pairs to be included in the Discovery Frame.
       If present it is always sent in a Discovery Frame
    */
    u16 tx_match_filter_len;
    u8 tx_match_filter[NAN_MAX_MATCH_FILTER_LEN];

    /*
       flag which specifies that the Publish should use the configured RSSI
       threshold and the received RSSI in order to filter requests
       0 – ignore the configured RSSI threshold when running a Service
           Descriptor attribute or Service ID List Attribute through the DE matching logic.
       1 – use the configured RSSI threshold when running a Service
           Descriptor attribute or Service ID List Attribute through the DE matching logic.

    */
    u8 rssi_threshold_flag;

    /*
       flag which control whether or not the Service is sent over the air
       in order to filter requests
       0 – Send the Publish Service ID over the air in both Service
       Discovery Frames, as well as, in the Service ID List Attribute
       in Sync/Discovery Beacons(assuming we are not NM-NS role).
            1 – Do not send the Publish Service ID over the air

    */
    u8 ota_flag;

    /*
       8-bit bitmap which allows the Host to associate this publish
       with a particular Post-NAN Connectivity attribute
       which has been sent down in a NanConfigureRequest/NanEnableRequest
       message.  If the DE fails to find a configured Post-NAN
       connectivity attributes referenced by the bitmap,
       the DE will return an error code to the Host.
       If the Publish is configured to use a Post-NAN Connectivity
       attribute and the Host does not refresh the Post-NAN Connectivity
       attribute the Publish will be canceled and the Host will be sent
       a PublishTerminatedIndication message.
    */
    u8 connmap;
} NanPublishRequest;

/*
  Publish Cancel Msg Structure
  The PublishServiceCancelReq Message is used to request the DE to stop publishing
  the Service Name identified by the handle in the message.
*/
typedef struct {
    NanHeader header;
} NanPublishCancelRequest;

/*
  NAN Subscribe Structure
  The SubscribeServiceReq message is sent to the Discovery Engine
  whenever the Upper layers would like to listen for a Service Name
*/
typedef struct {
    NanHeader header;
    u16 ttl; /* how many seconds to run for. 0 means forever until canceled */
    u16 period;/* periodicity of OTA Active Subscribe. Units in increments of 500 ms , 0 = attempt every DW*/

    /* Flag which specifies how the Subscribe request shall be processed. */
    u8 subscribe_type; /* 0 - PASSIVE , 1- ACTIVE */

    /* Flag which specifies on Active Subscribes how the Service Response Filter attribute is populated.*/
    u8 serviceResponseFilter; /* 0 - Bloom Filter, 1 - MAC Addr */

    /* Flag which specifies how the Service Response Filter Include bit is populated.*/
    u8 serviceResponseInclude; /* 0=Do not respond if in the Address Set, 1= Respond */

    /* Flag which specifies if the Service Response Filter should be used when creating Subscribes.*/
    u8 useServiceResponseFilter; /* 0=Do not send the Service Response Filter,1= send */

    /*
       Flag which specifies if the Service Specific Info is needed in
       the Publish message before creating the MatchIndication
    */
    u8 ssiRequiredForMatchIndication; /* 0=Not needed, 1= Required */

    /*
       Field which allows the matching behavior to be controlled.
       0 - Match Once
       1 - Match continuous
       2 - Match never
       3 - Reserved
    */
    NanMatchAlg subscribe_match;

    /*
       The number of Subscribe Matches which should occur
       before the Subscribe request is automatically terminated.
    */
    u8 subscribe_count; /* If this value is 0 this field is not used by the DE.*/

    u16 service_name_len;/* length of service name */
    u8 service_name[NAN_MAX_SERVICE_NAME_LEN]; /* UTF-8 encoded string identifying the service */

    /* Sequence of values which further specify the published service beyond the service name*/
    u16 service_specific_info_len;
    u8 service_specific_info[NAN_MAX_SERVICE_SPECIFIC_INFO_LEN];

    /*
       Ordered sequence of <length, value> pairs used to filter out received publish discovery messages.
       This can be sent both for a Passive or an Active Subscribe
    */
    u16 rx_match_filter_len;
    u8 rx_match_filter[NAN_MAX_MATCH_FILTER_LEN];

    /*
       Ordered sequence of <length, value> pairs  included in the
       Discovery Frame when an Active Subscribe is used.
    */
    u16 tx_match_filter_len;
    u8 tx_match_filter[NAN_MAX_MATCH_FILTER_LEN];

    /*
       Flag which specifies that the Subscribe should use the configured RSSI
       threshold and the received RSSI in order to filter requests
       0 – ignore the configured RSSI threshold when running a Service
           Descriptor attribute or Service ID List Attribute through the DE matching logic.
       1 – use the configured RSSI threshold when running a Service
           Descriptor attribute or Service ID List Attribute through the DE matching logic.

    */
    u8 rssi_threshold_flag;

    /*
       flag which control whether or not the Service is sent over the air
       in order to filter requests
       0 – Send the Publish Service ID over the air in both Service
       Discovery Frames, as well as, in the Service ID List Attribute
       in Sync/Discovery Beacons(assuming we are not NM-NS role).
            1 – Do not send the Publish Service ID over the air
    */
    u8 ota_flag;

    /*
       8-bit bitmap which allows the Host to associate this Active
       Subscribe with a particular Post-NAN Connectivity attribute
       which has been sent down in a NanConfigureRequest/NanEnableRequest
       message.  If the DE fails to find a configured Post-NAN
       connectivity attributes referenced by the bitmap,
       the DE will return an error code to the Host.
       If the Subscribe is configured to use a Post-NAN Connectivity
       attribute and the Host does not refresh the Post-NAN Connectivity
       attribute the Subscribe will be canceled and the Host will be sent
       a SubscribeTerminatedIndication message.
    */
    u8 connmap;
    /*
       NAN Interface Address, conforming to the format as described in
       8.2.4.3.2 of IEEE Std. 802.11-2012.
    */
    u8 num_intf_addr_present;
    u8 intf_addr[NAN_MAX_SUBSCRIBE_MAX_ADDRESS][NAN_MAC_ADDR_LEN];
} NanSubscribeRequest;


/*
  NAN Subscribe Cancel Structure
  The SubscribeCancelReq Message is used to request the DE to stop looking for the Service Name.
*/
typedef struct {
    NanHeader header;
} NanSubscribeCancelRequest;


/*
  Transmit follow up Structure
  The TransmitFollowupReq message is sent to the DE to allow the sending of the Service_Specific_Info
  to a particular MAC address.
*/
typedef struct {
    NanHeader header;
    /*
       A 32 bit Handle which is sent to the Application.  This handle will be
       sent in any subsequent UnmatchInd/FollowupInd messages
    */
    u32 match_handle;
    u8 addr[NAN_MAC_ADDR_LEN]; /* Can be a broadcast/multicast or unicast address */
    NanTxPriority priority; /* priority of the request 0 = low, 1=normal, 2=high */
    u8 dw_or_faw; /* 0= send in a DW, 1=send in FAW */

    /*
       Sequence of values which further specify the published service beyond the service name
       Treated as service specific info in case dw_or_faw is set to 0
       Treated as extended service specific info in case dw_or_faw is set to non-zero
    */
    u16 service_specific_info_len;
    u8 service_specific_info[NAN_MAX_SERVICE_SPECIFIC_INFO_LEN];
} NanTransmitFollowupRequest;

/*
  Stats Request structure
  The Discovery Engine can be queried at runtime by the Host processor for statistics
  concerning various parts of the Discovery Engine.
*/
typedef struct {
    NanHeader header;
    NanStatsId stats_id; /* NAN Statistics Request ID Codes */
    u8 clear; /* 0= Do not clear the stats and return the current contents , 1= Clear the associated stats  */
} NanStatsRequest;

/*
  Config Structure
  The NanConfigurationReq message is sent by the Host to the
  Discovery Engine in order to configure the Discovery Engine during runtime.
*/
typedef struct {
    NanHeader header;
    u8 config_sid_beacon;
    u8 sid_beacon; /* default = 0x01 */
    u8 config_rssi_proximity;
    u8 rssi_proximity; /* default = 70 (-70 dBm) */
    u8 config_random_time;
    u8 random_time; /* default  = 120 (DWs) */
    u8 config_master_pref;
    u8 master_pref; /* default = 0 */
    u8 config_periodic_scan_interval;
    u8 periodic_scan_interval; /* default = 20 seconds */
    /*
       The number of Additional Discovery Window slots in
       increments of 16 ms.  Since each DW is 512 TUs apart
       and the DW takes up 1 slot, the maximum number of additional
       slots which can be specified is 31.  This is a hint to the
       scheduler and there is no guarantee that all 31 slots will
       be available because of MCC and BT Coexistence channel usage
    */
    u8 additional_disc_window_slots; /* default = 0.*/

    /*
       1 byte signed quantity which defines the RSSI filter
       threshold.  Any Service Descriptors received above this
       value that are configured for RSSI filtering will be dropped.
    */
    u8 config_5g_rssi_close_proximity;
    u8 rssi_close_proximity_5g_val;
    /*
      Optional configuration of Configure request.
      Each of the optional parameters have configure flag which
      determine whether configuration is to be passed or not.
    */
    /*
       2 byte quantity which defines the window size over
       which the “average RSSI” will be calculated over.
    */
    u8 config_rssi_window_size;
    u16 rssi_window_size_val;
    /*
       If set to 1, the Discovery Engine will enclose the Cluster
       Attribute only sent in Beacons in a Vendor Specific Attribute
       and transmit in a Service Descriptor Frame.
    */
    u8 config_cluster_attribute_val;
    /*
      The periodicity in seconds between full scan’s to find any new
      clusters available in the area.  A Full scan should not be done
      more than every 10 seconds and should not be done less than every
      30 seconds.
    */
    u8 config_scan_params;
    NanSocialChannelScanParams scan_params_val;
    /*
      Debugging mode for Discovery engine
    */
    u8 config_debug_flags;
    u64 debug_flags_val;
    /*
       1 byte quantity which forces the Random Factor to a particular
       value for all transmitted Sync/Discovery beacons
    */
    u8 config_random_factor_force;
    u8 random_factor_force_val;
    /*
       1 byte quantity which forces the HC for all transmitted Sync and
       Discovery Beacon NO matter the real HC being received over the
       air.
    */
    u8 config_hop_count_force;
    u8 hop_count_force_val;
    /* NAN Post Connectivity Capability */
    u8 config_conn_capability;
    NanTransmitPostConnectivityCapability conn_capability_val;
    /* NAN Post Discover Capability */
    u8 config_discovery_attr;
    NanTransmitPostDiscovery discovery_attr_val;
    /* NAN Further availability Map */
    u8 config_fam;
    NanFurtherAvailabilityMap fam_val;
} NanConfigRequest;

/*
  TCA Structure
  The Discovery Engine can be configured to send up Events whenever a configured
  Threshold Crossing Alert (TCA) Id crosses an integral threshold in a particular direction.
*/
typedef struct {
    NanHeader header;
    NanTcaId tca_id; /* Nan Protocol Threshold Crossing Alert (TCA) Codes */

    /* flag which control whether or not an event is generated for the Rising direction */
    u8 rising_direction_evt_flag; /* 0 - no event, 1 - event */

    /* flag which control whether or not an event is generated for the Falling direction */
    u8 falling_direction_evt_flag;/* 0 - no event, 1 - event */

    /* flag which requests a previous TCA request to be cleared from the DE */
    u8 clear;/*0= Do not clear the TCA, 1=Clear the TCA */

    /* 32 bit value which represents the threshold to be used.*/
    u32 threshold;
} NanTCARequest;


/*
  Beacon Sdf Payload Structure
  The Discovery Engine can be configured to publish vendor specific attributes as part of
  beacon or service discovery frame transmitted as part of this request..
*/
typedef struct {
    NanHeader header;
    /*
       NanVendorAttribute will have the Vendor Specific Attribute which the
       vendor wants to publish as part of Discovery or Sync or Service discovery frame
    */
    NanTransmitVendorSpecificAttribute vsa;
} NanBeaconSdfPayloadRequest;

/* Publish statistics. */
typedef struct PACKED
{
    u32 validPublishServiceReqMsgs;
    u32 validPublishServiceRspMsgs;
    u32 validPublishServiceCancelReqMsgs;
    u32 validPublishServiceCancelRspMsgs;
    u32 validPublishRepliedIndMsgs;
    u32 validPublishTerminatedIndMsgs;
    u32 validActiveSubscribes;
    u32 validMatches;
    u32 validFollowups;
    u32 invalidPublishServiceReqMsgs;
    u32 invalidPublishServiceCancelReqMsgs;
    u32 invalidActiveSubscribes;
    u32 invalidMatches;
    u32 invalidFollowups;
    u32 publishCount;
} NanPublishStats;

/* Subscribe statistics. */
typedef struct PACKED
{
    u32 validSubscribeServiceReqMsgs;
    u32 validSubscribeServiceRspMsgs;
    u32 validSubscribeServiceCancelReqMsgs;
    u32 validSubscribeServiceCancelRspMsgs;
    u32 validSubscribeTerminatedIndMsgs;
    u32 validSubscribeMatchIndMsgs;
    u32 validSubscribeUnmatchIndMsgs;
    u32 validSolicitedPublishes;
    u32 validMatches;
    u32 validFollowups;
    u32 invalidSubscribeServiceReqMsgs;
    u32 invalidSubscribeServiceCancelReqMsgs;
    u32 invalidSubscribeFollowupReqMsgs;
    u32 invalidSolicitedPublishes;
    u32 invalidMatches;
    u32 invalidFollowups;
    u32 subscribeCount;
    u32 bloomFilterIndex;
} NanSubscribeStats;

/* NAN MAC Statistics. Used for MAC and DW statistics. */
typedef struct PACKED
{
    /* RX stats */
    u32 validFrames;
    u32 validActionFrames;
    u32 validBeaconFrames;
    u32 ignoredActionFrames;
    u32 ignoredBeaconFrames;
    u32 invalidFrames;
    u32 invalidActionFrames;
    u32 invalidBeaconFrames;
    u32 invalidMacHeaders;
    u32 invalidPafHeaders;
    u32 nonNanBeaconFrames;

    u32 earlyActionFrames;
    u32 inDwActionFrames;
    u32 lateActionFrames;

    /* TX stats */
    u32 framesQueued;
    u32 totalTRSpUpdates;
    u32 completeByTRSp;
    u32 completeByTp75DW;
    u32 completeByTendDW;
    u32 lateActionFramesTx;

    /* Misc stats - ignored for DW. */
    u32 twIncreases;
    u32 twDecreases;
    u32 twChanges;
    u32 twHighwater;
    u32 bloomFilterIndex;
} NanMacStats;

/* NAN Sync Statistics*/
typedef struct PACKED
{
    u64 currTsf;
    u64 myRank;
    u64 currAmRank;
    u64 lastAmRank;
    u32 currAmBTT;
    u32 lastAmBTT;
    u8  currAmHopCount;
    u8  currRole;
    u16 currClusterId;
    u32 reserved1;

    u64 timeSpentInCurrRole;
    u64 totalTimeSpentAsMaster;
    u64 totalTimeSpentAsNonMasterSync;
    u64 totalTimeSpentAsNonMasterNonSync;
    u32 transitionsToAnchorMaster;
    u32 transitionsToMaster;
    u32 transitionsToNonMasterSync;
    u32 transitionsToNonMasterNonSync;
    u32 amrUpdateCount;
    u32 amrUpdateRankChangedCount;
    u32 amrUpdateBTTChangedCount;
    u32 amrUpdateHcChangedCount;
    u32 amrUpdateNewDeviceCount;
    u32 amrExpireCount;
    u32 mergeCount;
    u32 beaconsAboveHcLimit;
    u32 beaconsBelowRssiThresh;
    u32 beaconsIgnoredNoSpace;
    u32 beaconsForOurCluster;
    u32 beaconsForOtherCluster;
    u32 beaconCancelRequests;
    u32 beaconCancelFailures;
    u32 beaconUpdateRequests;
    u32 beaconUpdateFailures;
    u32 syncBeaconTxAttempts;
    u32 syncBeaconTxFailures;
    u32 discBeaconTxAttempts;
    u32 discBeaconTxFailures;
    u32 amHopCountExpireCount;
} NanSyncStats;

/* NAN Misc DE Statistics */
typedef struct PACKED
{
    u32 validErrorRspMsgs;
    u32 validTransmitFollowupReqMsgs;
    u32 validTransmitFollowupRspMsgs;
    u32 validFollowupIndMsgs;
    u32 validConfigurationReqMsgs;
    u32 validConfigurationRspMsgs;
    u32 validStatsReqMsgs;
    u32 validStatsRspMsgs;
    u32 validEnableReqMsgs;
    u32 validEnableRspMsgs;
    u32 validDisableReqMsgs;
    u32 validDisableRspMsgs;
    u32 validDisableIndMsgs;
    u32 validEventIndMsgs;
    u32 validTcaReqMsgs;
    u32 validTcaRspMsgs;
    u32 validTcaIndMsgs;
    u32 invalidTransmitFollowupReqMsgs;
    u32 invalidConfigurationReqMsgs;
    u32 invalidStatsReqMsgs;
    u32 invalidEnableReqMsgs;
    u32 invalidDisableReqMsgs;
    u32 invalidTcaReqMsgs;
} NanDeStats;

/*
  Stats Response Message structure
  The Discovery Engine response to a request by the Host for statistics.
*/
typedef struct {
    NanStatsId stats_id;
    union {
        NanPublishStats publish_stats;
        NanSubscribeStats subscribe_stats;
        NanMacStats mac_stats;
        NanSyncStats sync_stats;
        NanDeStats de_stats;
    } data;
} NanStatsResponse;

/*
  NAN Response messages
*/
typedef struct {
    NanHeader header;
    u16 status; /* contains the result code */
    u16 value; /* For error returns the value is returned which was in error */
    NanResponseType response_type; /* NanResponseType Definitions */
    union {
        NanStatsResponse stats_response;
    } body;
} NanResponseMsg;


/*
  Publish Replied Indication
  The PublishRepliedInd Message is sent by the DE when an Active Subscribe is
  received over the air and it matches a Solicited PublishServiceReq which had
  been created with the replied_event_flag set.
*/
typedef struct {
    NanHeader header;
    u8 addr[NAN_MAC_ADDR_LEN];
    /*
       If RSSI filtering was configured in NanPublishRequest then this
       field will contain the received RSSI value. 0 if not
    */
    u8 rssi_value;
    /*
       optional attributes. Each optional attribute is associated with a flag
       which specifies whether the attribute is valid or not
    */
    /* NAN Post Connectivity Capability received */
    u8 is_conn_capability_valid;
    NanReceivePostConnectivityCapability conn_capability;

    /* NAN Post Discover Capability */
    u8 is_discovery_attr_valid;
    NanReceivePostDiscovery discovery_attr;

    /* NAN Further availability Map */
    u8 is_fam_valid;
    NanFurtherAvailabilityMap fam;

    /* NAN Cluster Attribute */
    u8 cluster_attribute_len;
    u8 cluster_attribute[NAN_MAX_CLUSTER_ATTRIBUTE_LEN];
} NanPublishRepliedInd;

/*
  Publish Terminated
  The PublishTerminatedInd message is sent by the DE whenever a Publish
  terminates from a user-specified timeout or a unrecoverable error in the DE.
*/
typedef struct {
    NanHeader header;
    NanTerminatedStatus reason;
} NanPublishTerminatedInd;

/*
  Match Indication
  The MatchInd message is sent once per responding MAC address whenever
  the Discovery Engine detects a match for a previous SubscribeServiceReq
  or PublishServiceReq.
*/
typedef struct {
    NanHeader header;

    /*
       A 32 bit Handle which is sent to the Application.  This handle will be
       sent in any subsequent UnmatchInd/FollowupInd messages
    */
    u32 match_handle;
    u8 addr[NAN_MAC_ADDR_LEN];

    /*
       Sequence of octets which were received in a Discovery Frame matching the
       Subscribe Request.
    */
    u16 service_specific_info_len;
    u8 service_specific_info[NAN_MAX_SERVICE_NAME_LEN];

    /*
       Ordered sequence of <length, value> pairs received in the Discovery Frame
       matching the Subscribe Request.
    */
    u16 sdf_match_filter_len;
    u8 sdf_match_filter[NAN_MAX_MATCH_FILTER_LEN];

    /*
       flag to indicate if the Match occurred in a Beacon Frame or in a
       Service Discovery Frame.
         0 - Match occured in a Service Discovery Frame
         1 - Match occured in a Beacon Frame
    */
    u8 match_occured_flag;

    /*
       flag to indicate FW is out of resource and that it can no longer
       track this Service Name. The Host still need to send the received
       Match_Handle but duplicate MatchInd messages may be received on
       this Handle until the resource frees up.
         0 - FW is caching this match
         1 - FW is unable to cache this match
    */
    u8 out_of_resource_flag;

    /*
       If RSSI filtering was configured in NanSubscribeRequest then this
       field will contain the received RSSI value. 0 if not.
    */
    u8 rssi_value;

    /*
       optional attributes. Each optional attribute is associated with a flag
       which specifies whether the attribute is valid or not
    */
    /* NAN Post Connectivity Capability received */
    u8 is_conn_capability_valid;
    NanReceivePostConnectivityCapability conn_capability;

    /* NAN Post Discover Capability */
    u8 is_discovery_attr_valid;
    NanReceivePostDiscovery discovery_attr;

    /* NAN Further availability Map */
    u8 is_fam_valid;
    NanFurtherAvailabilityMap fam;

    /* NAN Cluster Attribute */
    u8 cluster_attribute_len;
    u8 cluster_attribute[NAN_MAX_CLUSTER_ATTRIBUTE_LEN];
} NanMatchInd;

/*
  UnMatch Indication
  The UnmatchInd message is sent whenever the Discovery Engine detects that
  a previously Matched Service has been gone for too long. If the previous
  MatchInd message for this Handle had the out_of_resource_flag set then
  this message will not be received
*/
typedef struct {
    NanHeader header;
    /*
       32 bit value sent by the DE in a previous
       MatchInd/FollowupInd to the application.
    */
    u32 match_handle;
} NanUnmatchInd;

/*
  Subscribe Terminated
  The SubscribeTerminatedInd message is sent by the DE whenever a
  Subscribe terminates from a user-specified timeout or a unrecoverable error in the DE.
*/
typedef struct {
    NanHeader header;
    NanTerminatedStatus reason;
} NanSubscribeTerminatedInd;

/*
  Followup Indication Message
  The FollowupInd message is sent by the DE to the Host whenever it receives a
  Followup message from another peer.
*/
typedef struct {
    NanHeader header;
    /*
       A 32 bit Handle which is sent to the Application.  This handle will be
       sent in any subsequent UnmatchInd/FollowupInd messages
    */
    u32 match_handle;
    u8 addr[NAN_MAC_ADDR_LEN];

    /* Flag which the DE uses to decide if received in a DW or a FAW*/
    u8 dw_or_faw; /* 0=Received  in a DW, 1 = Received in a FAW*/

    /*
       Sequence of values which further specify the published service beyond the service name
       Service specific info in case dw_or_faw is set to 0
       Extended service specific info in case dw_or_faw is set to non-zero
    */
    u16 service_specific_info_len;
    u8 service_specific_info[NAN_MAX_SERVICE_SPECIFIC_INFO_LEN];
} NanFollowupInd;

/*
   Event data notifying the Mac address of the Discovery engine.
   which is reported as one of the Discovery engine event
*/
typedef struct {
    u8 addr[NAN_MAC_ADDR_LEN];
} NanMacAddressEvent;

/*
   Event data notifying the Cluster address of the cluster
   which is reported as one of the Discovery engine event
*/
typedef struct {
    u8 addr[NAN_MAC_ADDR_LEN];
} NanClusterEvent;

/*
  Discovery Engine Event Indication
  The Discovery Engine can inform the Host when significant events occur
  The data following the EventId is dependent upon the EventId type.
  In other words, each new event defined will carry a different
  structure of information back to the host.
*/
typedef struct {
    NanHeader header;
    NanEventId event_id; /* NAN Protocol Event Codes */
    union {
        /*
           MacAddressEvent which will have 6 byte mac address
           of the Discovery engine.
        */
        NanMacAddressEvent mac_addr;
        /*
           Cluster Event Data which will be obtained when the
           device starts a new cluster or joins a cluster.
           The event data will have 6 byte octet string of the
           cluster started or joined.
        */
        NanClusterEvent cluster;
    } data;
} NanDiscEngEventInd;

/* Cluster size TCA event*/
typedef struct {
    /* size of the cluster*/
    u32 cluster_size;
} NanTcaClusterEvent;

/*
  NAN TCA Indication
  The Discovery Engine can inform the Host when significant events occur.
  The data following the TcaId is dependent upon the TcaId type.
  In other words, each new event defined will carry a different structure
  of information back to the host.
*/
typedef struct {
    NanHeader header;
    NanTcaId tca_id;
    /* flag which defines if the configured Threshold has risen above the threshold */
    u8 rising_direction_evt_flag; /* 0 - no event, 1 - event */

    /* flag which defines if the configured Threshold has fallen below the threshold */
    u8 falling_direction_evt_flag;/* 0 - no event, 1 - event */
    union {
        /*
           This event in obtained when the cluser size threshold
           is crossed. Event will have the cluster size
        */
        NanTcaClusterEvent cluster;
    } data;
} NanTCAInd;

/*
  NAN Disabled Indication
  The NanDisableInd message indicates to the upper layers that the Discovery
  Engine has flushed all state and has been shutdown.  When this message is received
  the DE is guaranteed to have left the NAN cluster it was part of and will have terminated
  any in progress Publishes or Subscribes.
*/
typedef struct {
    NanHeader header;
    NanStatusType reason;
} NanDisabledInd;

/*
  NAN Beacon SDF Payload Indication
  The NanBeaconSdfPayloadInd message indicates to the upper layers that information
  elements were received either in a Beacon or SDF which needs to be delivered
  outside of a Publish/Subscribe Handle.
*/
typedef struct {
    NanHeader header;
    /* The MAC address of the peer which sent the attributes.*/
    u8 addr[NAN_MAC_ADDR_LEN];
    /*
       Optional attributes. Each optional attribute is associated with a flag
       which specifies whether the attribute is valid or not
    */
    /* NAN Receive Vendor Specific Attribute*/
    u8 is_vsa_received;
    NanReceiveVendorSpecificAttribute vsa;

    /* NAN Beacon SDF Payload Received*/
    u8 is_beacon_sdf_payload_received;
    NanBeaconSdfPayloadReceive data;
} NanBeaconSdfPayloadInd;

typedef struct {
  u64 master_rank;
  u8 master_pref;
  u8 random_factor;
  u8 hop_count;
  u32 beacon_transmit_time;
} NanStaParameter;

/* Response and Event Callbacks */
typedef struct {
    /* NotifyResponse invoked to notify the status of the Request */
    void (*NotifyResponse)(NanResponseMsg* rsp_data,
                           void* userdata);
    /* Various Events Callback */
    void (*EventPublishReplied)(NanPublishRepliedInd* event,
                                void* userdata);
    void (*EventPublishTerminated)(NanPublishTerminatedInd* event,
                                   void* userdata);
    void (*EventMatch) (NanMatchInd* event,
                        void* userdata);
    void (*EventUnMatch) (NanUnmatchInd* event,
                          void* userdata);
    void (*EventSubscribeTerminated) (NanSubscribeTerminatedInd* event,
                                      void* userdata);
    void (*EventFollowup) (NanFollowupInd* event,
                           void* userdata);
    void (*EventDiscEngEvent) (NanDiscEngEventInd* event,
                               void* userdata);
    void (*EventDisabled) (NanDisabledInd* event,
                           void* userdata);
    void (*EventTca) (NanTCAInd* event,
                      void* userdata);
    void (*EventSdfPayload) (NanBeaconSdfPayloadInd* event,
                             void* userdata);
} NanCallbackHandler;


/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_enable_request(wifi_request_id id,
                              wifi_handle handle,
                              NanEnableRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_disable_request(wifi_request_id id,
                               wifi_handle handle,
                               NanDisableRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_publish_request(wifi_request_id id,
                               wifi_handle handle,
                               NanPublishRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_publish_cancel_request(wifi_request_id id,
                                      wifi_handle handle,
                                      NanPublishCancelRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_subscribe_request(wifi_request_id id,
                                 wifi_handle handle,
                                 NanSubscribeRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_subscribe_cancel_request(wifi_request_id id,
                                        wifi_handle handle,
                                        NanSubscribeCancelRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_transmit_followup_request(wifi_request_id id,
                                         wifi_handle handle,
                                         NanTransmitFollowupRequest* msg);

/*  Function to send NAN statistics request to the wifi driver.*/
wifi_error nan_stats_request(wifi_request_id id,
                             wifi_handle handle,
                             NanStatsRequest* msg);

/*  Function to send NAN configuration request to the wifi driver.*/
wifi_error nan_config_request(wifi_request_id id,
                              wifi_handle handle,
                              NanConfigRequest* msg);

/*  Function to send NAN request to the wifi driver.*/
wifi_error nan_tca_request(wifi_request_id id,
                           wifi_handle handle,
                           NanTCARequest* msg);

/*
    Function to send NAN Beacon sdf payload to the wifi driver.
    This instructs the Discovery Engine to begin publishing the
    received payload in any Beacon or Service Discovery Frame
    transmitted
*/
wifi_error nan_beacon_sdf_payload_request(wifi_request_id id,
                                         wifi_handle handle,
                                         NanBeaconSdfPayloadRequest* msg);
/*
    Function to get the sta_parameter expected by Sigma
    as per CAPI spec.
*/
wifi_error nan_get_sta_parameter(wifi_request_id id,
                                 wifi_handle handle,
                                 NanStaParameter* msg);

/*  Function to register NAN callback */
wifi_error nan_register_handler(wifi_handle handle,
                                NanCallbackHandler handlers,
                                void* userdata);

/*  Function to get version of the NAN HAL */
wifi_error nan_get_version(wifi_handle handle,
                           NanVersion* version);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __NAN_H__ */
