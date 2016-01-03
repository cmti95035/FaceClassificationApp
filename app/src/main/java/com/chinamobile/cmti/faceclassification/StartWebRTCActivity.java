package com.chinamobile.cmti.faceclassification;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chinamobile.cmti.faceclassification.R;
import com.chinamobile.cmti.faceclassification.model.ConfigData;
import com.att.webrtcsdk.MediaType;
import com.att.webrtcsdk.apicall.Constants;
import com.att.webrtcsdk.apicall.SdkCallbacks;
import com.att.webrtcsdk.model.OAuthToken;
import com.att.webrtcsdk.phone.CallConnectedEvent;
import com.att.webrtcsdk.phone.InvitationEvent;
import com.att.webrtcsdk.phone.Phone;
import com.att.webrtcsdk.phone.PhoneCallbacks;
import com.att.webrtcsdk.phone.PhoneErrorType;
import com.att.webrtcsdk.phone.PhoneEventAdapter;
import com.att.webrtcsdk.phone.PhoneEventListener;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StartWebRTCActivity extends AppCompatActivity {

    private Phone phone;
    private PhoneEventListener phoneEventListener;

    private ApiRequest apiRequest;
    private TextView sessionId;
    private TextView domain;
    private EditText userId;
    private EditText phone_number;
    private String access_token;
    private InvitationEvent invitationEvent;
    private String TAG = StartWebRTCActivity.class.getSimpleName();

    private static final String VISITOR = "visitor";
    private static final String CMTI = "cmti";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);

        // Required - Set, if the DHS is not run in local
//        Constants.setBaseURL("https://ec2-52-53-232-49.us-west-1.compute.amazonaws.com:9001");
        Constants.setBaseURL("http://devsummit-ewebrtc.herokuapp.com");
//        Constants.setSocketUrl("ws://ec2-52-53-232-49.us-west-1.compute.amazonaws.com:9001");
        Constants.setSocketUrl("ws://devsummit-ewebrtc.herokuapp.com");

        apiRequest = new ApiRequest();

        sessionId = (TextView) findViewById(R.id.sessionId);
        domain = (TextView) findViewById(R.id.domain);

        userId = (EditText) findViewById(R.id.userId);
        phone_number = (EditText) findViewById(R.id.phone_number);

        phoneEventListener = new PhoneEventAdapter() {
            @Override
            public void onError(PhoneErrorType type, String error) {
                super.onError(type, error);
            }

            @Override
            public void onWarning(String warning) {
                super.onWarning(warning);
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
            }

            @Override
            public void onCallStateChange(Phone.CallState callState) {
                super.onCallStateChange(callState);
            }

            @Override
            public void onPhoneStateChange(Phone.PhoneState phoneState) {
                super.onPhoneStateChange(phoneState);
            }

            @Override
            public void onSessionReady() {
                super.onSessionReady();

                final Intent serviceIntent = new Intent(getApplicationContext(), RTCService.class);
                startService(serviceIntent);

//                ((TextView) findViewById(R.id.sessionId)).setText(phone.getSessionId());
//                findViewById(R.id.sessionId).setVisibility(View.VISIBLE);
//                findViewById(R.id.phone_number).setVisibility(View.VISIBLE);
//                findViewById(R.id.video_button).setVisibility(View.VISIBLE);
//                findViewById(R.id.audio_button).setVisibility(View.VISIBLE);
//                findViewById(R.id.logout_button).setVisibility(View.VISIBLE);

                Intent idCallIntent = new Intent(getApplicationContext(), RTCActivity.class);
                String validatedPhoneNumber = Utility.validatePhoneNumber(CMTI);
                if (null != validatedPhoneNumber) {
                    //Valid PhoneNumber
                    idCallIntent.putExtra("DESTINATION", "tel:" + validatedPhoneNumber);
                    idCallIntent.putExtra("MEDIATYPE", MediaType.AUDIO);
                    startActivity(idCallIntent);
                } else {

                    //TODO : validate emailID or UserID with domain if needed
                    String userId = "cmti";
                    idCallIntent.putExtra("DESTINATION", "sip:" + userId + '@' + domain.getText());
                    idCallIntent.putExtra("MEDIATYPE", MediaType.AUDIO_VIDEO);
                    startActivity(idCallIntent);
                }
            }

            @Override
            public void onSessionDisconnected() {
                super.onSessionDisconnected();
                stopService(new Intent(getApplicationContext(), RTCService.class));
                finish();
            }

            @Override
            public void onSessionExpired() {
                super.onSessionExpired();
            }

            @Override
            public void onInvitationReceived(InvitationEvent event) {
                super.onInvitationReceived(event);

                findViewById(R.id.answer_button).setVisibility(View.VISIBLE);
                findViewById(R.id.reject_button).setVisibility(View.VISIBLE);

                invitationEvent = event;
            }

            @Override
            public void onCallConnecting(MediaType mediaType) {
                super.onCallConnecting(mediaType);
            }

            @Override
            public void onCallCanceled(String callId) {
                super.onCallCanceled(callId);
            }

            @Override
            public void onCallConnected(CallConnectedEvent event) {
                super.onCallConnected(event);
            }

            @Override
            public void onCallRejected(String callId) {
                super.onCallRejected(callId);
            }

            @Override
            public void onCallDisconnected(String callId) {
                findViewById(R.id.answer_button).setVisibility(View.INVISIBLE);
                findViewById(R.id.reject_button).setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCallUserNotFound() {
                super.onCallUserNotFound();
            }
        };

        phone = Phone.getPhone(getApplicationContext());
        phone.registerEventListener(phoneEventListener);

        apiRequest.getConfig(new SdkCallbacks.SuccessCallback() {
            @Override
            public void onSuccess(Object object) {
                ConfigData gd = (ConfigData) object;
                ((TextView) findViewById(R.id.domain)).setText(gd.getEwebrtcDomain());

                apiRequest.getOAuthToken(new SdkCallbacks.SuccessCallback() {
                    @Override
                    public void onSuccess(Object token) {
                        OAuthToken ot = (OAuthToken) token;
                        ((TextView) findViewById(R.id.access_token)).setText(ot.getAccessToken());

                        access_token = ot.getAccessToken();

                        phone.associateAccessToken(VISITOR, access_token, new PhoneCallbacks.SuccessCallback() {
                            @Override
                            public void onSuccess() {
                                phone.logout();
                                phone.login(access_token);
                            }
                        }, new PhoneCallbacks.ErrorCallback() {
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error while associate User " + error);
                            }
                        });
                    }
                }, new SdkCallbacks.ErrorCallback() {
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error while getting Access Token " + error);
                    }
                });
            }
        }, new SdkCallbacks.ErrorCallback() {
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error while getting the config " + error);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.answer_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.reject_button).setVisibility(View.INVISIBLE);
    }

    public void answerCall(View v) {
        Intent intent = new Intent(getApplicationContext(), RTCActivity.class);
        if (invitationEvent.getMediaType() == MediaType.AUDIO_VIDEO) {
            intent.putExtra("MEDIATYPE", MediaType.AUDIO_VIDEO);
        } else {
            intent.putExtra("MEDIATYPE", MediaType.AUDIO);
        }
        intent.putExtra("OFFER_SDP", invitationEvent.getSdp());
        intent.putExtra("CALL_ID", invitationEvent.getCallId());
        startActivity(intent);
    }

    public void rejectCall(View v) {
        phone.rejectCall(invitationEvent);
    }

    public void getConfig(View v) {
        apiRequest.getConfig(new SdkCallbacks.SuccessCallback() {
            @Override
            public void onSuccess(Object object) {
                ConfigData gd = (ConfigData) object;
                ((TextView) findViewById(R.id.domain)).setText(gd.getEwebrtcDomain());
                findViewById(R.id.get_token).setVisibility(View.VISIBLE);
                findViewById(R.id.access_token).setVisibility(View.VISIBLE);
                findViewById(R.id.get_config_button).setEnabled(false);
            }
        }, new SdkCallbacks.ErrorCallback() {
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error while getting the config " + error);
            }
        });
    }

    public void getAccessToken(View v) {
        apiRequest.getOAuthToken(new SdkCallbacks.SuccessCallback() {
            @Override
            public void onSuccess(Object token) {
                OAuthToken ot = (OAuthToken) token;
                ((TextView) findViewById(R.id.access_token)).setText(ot.getAccessToken());

                access_token = ot.getAccessToken();
                findViewById(R.id.userId).setVisibility(View.VISIBLE);
                findViewById(R.id.domain).setVisibility(View.VISIBLE);
                findViewById(R.id.associate_user_button).setVisibility(View.VISIBLE);
                findViewById(R.id.get_token).setEnabled(false);

            }
        }, new SdkCallbacks.ErrorCallback() {
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error while getting Access Token " + error);
            }
        });
    }

    public void associateUser(View v) {
//        phone.associateAccessToken(userId.getText().toString(), access_token, new PhoneCallbacks.SuccessCallback() {
        phone.associateAccessToken("visitor", access_token, new PhoneCallbacks.SuccessCallback() {
            @Override
            public void onSuccess() {
                findViewById(R.id.login_button).setVisibility(View.VISIBLE);
                findViewById(R.id.associate_user_button).setEnabled(false);
            }
        }, new PhoneCallbacks.ErrorCallback() {
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error while associate User " + error);
            }
        });
    }

    public void login(View v) {
        phone.login(access_token);
    }

    public void dial(View v) {
        MediaType mediaType = MediaType.AUDIO;
        Log.d("Button Type ", v.getId() + "");

        if (v.getId() == R.id.audio_button) {
            mediaType = MediaType.AUDIO;
        } else {
            mediaType = MediaType.AUDIO_VIDEO;
        }

        if (0 == phone_number.getText().toString().length()) {
            Toast.makeText(getApplicationContext(), "Enter a valid callee ", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent idCallIntent = new Intent(getApplicationContext(), RTCActivity.class);
        String validatedPhoneNumber = Utility.validatePhoneNumber(phone_number.getText().toString());
        if (null != validatedPhoneNumber) {
            //Valid PhoneNumber
            idCallIntent.putExtra("DESTINATION", "tel:" + validatedPhoneNumber);
            idCallIntent.putExtra("MEDIATYPE", mediaType);
            startActivity(idCallIntent);
        } else {

            //TODO : validate emailID or UserID with domain if needed
            String userId = phone_number.getText().toString();
            idCallIntent.putExtra("DESTINATION", "sip:" + userId + '@' + domain.getText());
            idCallIntent.putExtra("MEDIATYPE", mediaType);
            startActivity(idCallIntent);
        }


    }

    public void logout(View v) {
        phone.logout();
    }

}
