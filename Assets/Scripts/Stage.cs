using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Stage : MonoBehaviour
{
    bool _lerpRunning = false;

    const int ROTATION_DEGREES = 60;
    const float DURATION_OF_ROTATION = .5f;

    Coroutine currentCoroutine;

    public void Rotate(bool left)
    {
        float rotation = ROTATION_DEGREES;
        if (left) rotation *= -1;

        if (!_lerpRunning)
        {
            if (currentCoroutine != null)
            {
                StopCoroutine(currentCoroutine);
            }
            currentCoroutine = StartCoroutine(RotationLerp(this.transform, rotation, DURATION_OF_ROTATION));
        }
    }

    private IEnumerator RotationLerp(Transform transform, float rotation, float duration)
    {
        _lerpRunning = true;
        Quaternion initialRotation = transform.rotation;
        Quaternion rotationTarget = Quaternion.Euler(0,0,transform.eulerAngles.z + rotation);
        float t = 0;
        while(t < duration)
        {
            t += Time.deltaTime;
            transform.rotation = Quaternion.Slerp(initialRotation, rotationTarget, t/duration);
            yield return null;
        }
        _lerpRunning = false;
        yield return null;
    }
}
